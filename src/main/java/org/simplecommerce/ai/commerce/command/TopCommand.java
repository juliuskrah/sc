package org.simplecommerce.ai.commerce.command;

import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.stereotype.Component;

import org.simplecommerce.ai.commerce.chat.ChatCommand;
import org.simplecommerce.ai.commerce.config.ConfigCommand;
import org.simplecommerce.ai.commerce.logs.LogsCommand;
import org.simplecommerce.ai.commerce.ls.ListCommand;
import org.simplecommerce.ai.commerce.serve.ServeCommand;
import org.simplecommerce.ai.commerce.stop.StopCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Spec;

@Component
@Command(
    name = "sc",
    description = "A runtime for AI chatbots",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = {
        ChatCommand.class,
        ConfigCommand.class,
        CommandLine.HelpCommand.class,
        LogsCommand.class,
        ListCommand.class,
        ServeCommand.class,
        StopCommand.class,
    }
)
public class TopCommand implements Runnable {
    @Spec
    CommandLine.Model.CommandSpec spec;
    @Mixin
    OllamaMixin ollamaMixin;
    private final OllamaConnectionProperties ollamaConnectionProperties;

    public TopCommand(OllamaConnectionProperties ollamaConnectionProperties) {
        this.ollamaConnectionProperties = ollamaConnectionProperties;
    }

    @Override
    public void run() {
        // If no subcommand is provided, print the help message
        spec.commandLine().usage(System.out);
    }

    public String getEndpoint() {
        return ollamaConnectionProperties.getBaseUrl();
    }
}
