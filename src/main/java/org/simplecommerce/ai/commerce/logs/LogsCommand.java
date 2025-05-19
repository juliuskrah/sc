package org.simplecommerce.ai.commerce.logs;

import org.springframework.stereotype.Component;

import org.simplecommerce.ai.commerce.command.ChatbotVersionProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "logs",
    description = "Fetch the logs of a service",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = {
        CommandLine.HelpCommand.class
    }
)
@Component
public class LogsCommand implements Runnable {
    @Parameters(
        arity= "1",
        paramLabel="SERVICE",
        description = "The ID or name of the service to fetch logs for"
    )
    private String serviceIdentifier;
    @Option(
        names = {"-f", "--follow"},
        description = "Follow the logs"
    )
    private boolean follow;

    @Override
    public void run() {
        // Fetch logs for the specified service
    }

}
