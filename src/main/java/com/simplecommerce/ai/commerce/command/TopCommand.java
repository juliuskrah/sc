package com.simplecommerce.ai.commerce.command;

import org.springframework.stereotype.Component;

import com.simplecommerce.ai.commerce.chat.ChatCommand;
import com.simplecommerce.ai.commerce.logs.LogsCommand;
import com.simplecommerce.ai.commerce.ls.ListCommand;
import com.simplecommerce.ai.commerce.serve.ServeCommand;
import com.simplecommerce.ai.commerce.stop.StopCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

@Component
@Command(
    name = "sc",
    description = "A runtime for AI chatbots",
    mixinStandardHelpOptions = true,
    versionProvider = CommerceVersionProvider.class,
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

    @Override
    public void run() {
        String greeting = String.format("Show usage help");
        System.out.println(Ansi.AUTO.string(greeting));
        System.out.flush();
    }
}
