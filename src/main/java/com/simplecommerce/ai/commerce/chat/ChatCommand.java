package com.simplecommerce.ai.commerce.chat;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(
    name = "chat",
    description = "Chat with a bot",
    mixinStandardHelpOptions = true,
    subcommands = CommandLine.HelpCommand.class
)
public class ChatCommand implements Runnable {
    private final ChatModel chatModel;

    public ChatCommand(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Parameters(
        arity = "0..1",
        paramLabel = "MESSAGE",
        description = "Message to send"
    )
    private String message;

    @Option(
        names = {"-m", "--model"},
        paramLabel = "MODEL",
        defaultValue = "mistral-small3.1",
        description = "Specify LLM to use"
    )
    private String model;

    @Override
    public void run() {
        var streamingResponse = chatModel.stream(message);
        streamingResponse.toStream().forEach(chunk -> {
            System.out.print(Ansi.AUTO.string(chunk));
            System.out.flush();
        });
        System.out.println(); // Add a newline at the end
    }
}
