package org.sc.ai.cli.command;

import org.sc.ai.cli.chat.ChatCommand;
import org.sc.ai.cli.config.ConfigCommand;
import org.sc.ai.cli.rag.RagCommand;
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
    ProviderMixin ollamaMixin;
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
