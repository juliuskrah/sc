package com.simplecommerce.ai.commerce.serve;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import com.simplecommerce.ai.commerce.service.ServiceInfo;
import com.simplecommerce.ai.commerce.service.ServiceRegistry;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

@Component
@Command(
    name = "serve",
    description = "Start a web service",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = CommandLine.HelpCommand.class
)
public class ServeCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ServeCommand.class);
    private final ServiceRegistry serviceRegistry;

    public ServeCommand(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Option(
        names = {"-d", "--detach"},
        description = "Run a service in the background and print service ID"
    )
    private boolean detach;

    @Option(
        names = {"-p", "--port"},
        paramLabel = "PORT",
        defaultValue = "8080",
        description = "Specify the port for the application"
    )
    private int port;

    @Option(
        names = {"--name"},
        paramLabel = "NAME",
        description = "Assign a name to the service"
    )
    private String name;

    @Override
    public void run() {
        try {
            // Build the command to start a new Spring Boot application
            List<String> command = new ArrayList<>();
            command.add(System.getProperty("java.home") + "/bin/java");
            command.add("-jar");
            command.add(System.getProperty("user.dir") + "/build/libs/commerce-ai-0.0.1-SNAPSHOT.jar");
            command.add("--server.port=" + port);
            
            // Create process builder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            
            // Create logs directory if it doesn't exist
            new File("logs").mkdirs();
            
            // Redirect output based on detach mode
            if (detach) {
                processBuilder.redirectOutput(new File("logs/service-" + port + ".log"));
                processBuilder.redirectError(new File("logs/service-" + port + ".error.log"));
            } else {
                processBuilder.inheritIO();
            }
            
            // Start the process
            Process process = processBuilder.start();
            
            // Create and register service info
            ServiceInfo serviceInfo = new ServiceInfo(name != null ? name : "service-" + port, port, process);
            serviceRegistry.registerService(serviceInfo);
            
            // Print service information
            String message = String.format("Started service '%s' (ID: %s) on port %d%s", 
                serviceInfo.getName(),
                serviceInfo.getId(),
                port,
                detach ? " in detached mode" : "");
            logger.info(message);
            System.out.println(Ansi.AUTO.string(message)); // Keep console output for CLI
            
            // If not detached, wait for the process
            if (!detach) {
                process.waitFor();
                serviceInfo.setRunning(false);
                serviceRegistry.unregisterService(serviceInfo.getId());
                logger.info("Service {} stopped", serviceInfo.getId());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to start service: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
