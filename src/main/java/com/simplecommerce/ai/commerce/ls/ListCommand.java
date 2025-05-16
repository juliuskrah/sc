package com.simplecommerce.ai.commerce.ls;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import com.simplecommerce.ai.commerce.service.ServiceInfo;
import com.simplecommerce.ai.commerce.service.ServiceRegistry;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

@Command(
    name = "ps",
    aliases = {"ls", "list"},
    description = "List running services",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = CommandLine.HelpCommand.class
)
@Component
public class ListCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);
    private final ServiceRegistry serviceRegistry;

    public ListCommand(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void run() {
        Map<String, ServiceInfo> services = serviceRegistry.getAllServices();
        
        if (services.isEmpty()) {
            System.out.println(Ansi.AUTO.string("No running services"));
            return;
        }

        // Print header
        System.out.println(Ansi.AUTO.string(String.format("%-36s %-20s %-10s %-15s", "ID", "NAME", "PORT", "UPTIME")));
        System.out.println(Ansi.AUTO.string("-".repeat(85)));

        // Print each service
        services.values().forEach(service -> {
            Duration uptime = Duration.between(service.getStartTime(), Instant.now());
            String uptimeStr = String.format("%02d:%02d:%02d", 
                uptime.toHours(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());

            System.out.println(Ansi.AUTO.string(String.format("%-36s %-20s %-10d %-15s",
                service.getId(),
                service.getName(),
                service.getPort(),
                uptimeStr)));
        });
    }
}
