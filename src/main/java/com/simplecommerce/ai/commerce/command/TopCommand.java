package com.simplecommerce.ai.commerce.command;

import org.springframework.stereotype.Component;

import com.simplecommerce.ai.commerce.chat.ChatCommand;
import com.simplecommerce.ai.commerce.logs.LogsCommand;
import com.simplecommerce.ai.commerce.ls.ListCommand;
import com.simplecommerce.ai.commerce.serve.ServeCommand;
import com.simplecommerce.ai.commerce.stop.StopCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@Component
@Command(
    name = "sc",
    description = "A runtime for AI chatbots",
    mixinStandardHelpOptions = true,
    versionProvider = ChatbotVersionProvider.class,
    subcommands = {
        ChatCommand.class,
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

    @Override
    public void run() {
        // If no subcommand is provided, print the help message
        spec.commandLine().usage(System.out);
    }
}
