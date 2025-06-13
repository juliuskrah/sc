package org.simplecommerce.ai.commerce.command;

import org.simplecommerce.ai.commerce.chat.ChatCommand;
import org.simplecommerce.ai.commerce.config.ConfigCommand;
import org.simplecommerce.ai.commerce.rag.RagCommand;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.stereotype.Component;

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
        RagCommand.class,
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
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public String getEndpoint() {
        return ollamaConnectionProperties.getBaseUrl();
    }
}
