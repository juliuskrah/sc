package org.simplecommerce.ai.commerce.stop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import org.simplecommerce.ai.commerce.service.ServiceInfo;
import org.simplecommerce.ai.commerce.service.ServiceRegistry;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;

@Command(
    name = "stop",
    description = "Stop a running service",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = CommandLine.HelpCommand.class
)
@Component
public class StopCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);
    private final ServiceRegistry serviceRegistry;

    @Parameters(
        arity = "1",
        paramLabel = "SERVICE",
        description = "The name or ID of the service to stop"
    )
    private String serviceIdentifier;

    public StopCommand(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void run() {
        ServiceInfo service = serviceRegistry.getService(serviceIdentifier);
        
        if (service == null) {
            // Try finding by name if not found by ID
            service = serviceRegistry.getAllServices().values().stream()
                .filter(s -> s.getName().equals(serviceIdentifier))
                .findFirst()
                .orElse(null);
        }

        if (service == null) {
            logger.error("Service not found: {}", serviceIdentifier);
            System.err.println(Ansi.AUTO.string("Service not found: " + serviceIdentifier));
            return;
        }

        try {
            Process process = service.getProcess();
            if (process.isAlive()) {
                process.destroy();
                // Give it a chance to shut down gracefully
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            }
            
            service.setRunning(false);
            serviceRegistry.unregisterService(service.getId());
            
            String message = String.format("Service '%s' (ID: %s) stopped", service.getName(), service.getId());
            logger.info(message);
            System.out.println(Ansi.AUTO.string(message));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error stopping service: {}", e.getMessage());
            System.err.println(Ansi.AUTO.string("Error stopping service: " + e.getMessage()));
        }
    }
}
