package com.simplecommerce.ai.commerce.command;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(
    name = "sc",
    description = "Simple Commerce",
    mixinStandardHelpOptions = true,
    versionProvider = CommerceVersionProvider.class,
    subcommands = CommandLine.HelpCommand.class
)
public class CommerceCommand implements Runnable {
    private final ChatModel chatModel;

    public CommerceCommand(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @Option(names = {"-n", "--name"}, paramLabel="NAME", description = "Your name")
    private String name = "User";

    @Command(
        mixinStandardHelpOptions = true,
        name = "chat",
        description = "Chat with a bot",
        subcommands = CommandLine.HelpCommand.class
    )
    public void chat(
        @Parameters(
            arity        = "0..1",
            paramLabel   = "MESSAGE",
            description  = "Message to send") String message, 
        @Option(
            names        = {"-m", "--model"},
            paramLabel   = "MODEL",
            defaultValue = "mistral-small3.1",
            description  = "Specify LLM to use"
        ) String model) {
            var streamingResponse = chatModel.stream(message);
            streamingResponse.toStream().forEach(chunk -> {
            System.out.print(Ansi.AUTO.string(chunk));
            System.out.flush();
        });
        System.out.println(); // Add a newline at the end
    }

    @Override
    public void run() {
        String greeting = String.format("Welcome to Simple Commerce, %s how may I help you?", name);
        System.out.println(Ansi.AUTO.string(greeting));
    }
}
