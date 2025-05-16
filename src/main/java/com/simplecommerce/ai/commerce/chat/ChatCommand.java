package com.simplecommerce.ai.commerce.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.springframework.stereotype.Component;

import com.simplecommerce.ai.commerce.command.ChatbotVersionProvider;

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
    versionProvider = ChatbotVersionProvider.class,
    subcommands = CommandLine.HelpCommand.class
)
public class ChatCommand implements Runnable {
    private final ChatService chatService;
    private final LineReader reader;
    private PrintWriter out = new PrintWriter(System.out, false);
    private static final String PROMPT = "sc> ";

    public ChatCommand(ChatService chatService, LineReader reader) {
        this.chatService = chatService;
        this.reader = reader;
    }

    private String fromPipe() {
        var messageBuilder = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                messageBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messageBuilder.toString().trim();
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
        if (message == null || message.isEmpty()) {
            message = fromPipe();
        }
        var streamingResponse = chatService.sendAndStreamMessage(message, model);
        streamingResponse.toStream().forEach(chunk -> {
            out.print(Ansi.AUTO.string(chunk));
            out.flush();
        });
        out.println();
        // System.out.println("Terminal type: " + reader.getTerminal().getClass().getSimpleName());
        // out = reader.getTerminal().writer();
        // while (true) {
        //     var line = reader.readLine(PROMPT);
        //     if ("exit".equals(line) || "quit".equals(line)) {
        //         break;
        //     }
        //     out.println("You entered: " + line);
        //     reader.getTerminal().flush();
        // }
    }
}
