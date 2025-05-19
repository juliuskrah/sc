package org.simplecommerce.ai.commerce.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Optional;

import org.jline.reader.LineReader;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.stereotype.Component;

import org.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import org.simplecommerce.ai.commerce.command.OllamaMixin;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "chat", description = "Chat with a bot", mixinStandardHelpOptions = true, versionProvider = ChatbotVersionProvider.class, subcommands = CommandLine.HelpCommand.class)
public class ChatCommand implements Runnable {
    private static final String PROMPT = "sc> ";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ChatCommand.class);
    private final ChatService chatService;
    private final OllamaChatProperties ollamaChatProperties;
    private final LineReader reader;
    private PrintWriter out = new PrintWriter(System.out, false);
    @Parameters(arity = "0..1", paramLabel = "MESSAGE", description = "Message to send")
    private String message;

    @Option(names = { "-m",
            "--model" }, paramLabel = "MODEL", description = "Specify LLM to use")
    private String model;
    @Mixin
    private OllamaMixin ollamaMixin;

    public ChatCommand(ChatService chatService, LineReader reader, OllamaChatProperties ollamaChatProperties) {
        this.chatService = chatService;
        this.reader = reader;
        this.ollamaChatProperties = ollamaChatProperties;
    }

    private String fromStdIn() throws IOException {
        var messageBuilder = new StringBuilder();
        if(System.in.available() == 0) {
            return null;
        }
        try (var bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                messageBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messageBuilder.toString().trim();
    }

    @Override
    public void run() {
        if (message == null) {
            try {
                message = fromStdIn();
            } catch (IOException e) {
                logger.warn("Error reading from stdin", e);
            }
        }
        model = Optional.ofNullable(model).orElse(ollamaChatProperties.getModel());
        logger.debug("Using chat model: {}", model);
        if (message == null || message.isBlank()) {
            out = reader.getTerminal().writer();
            reader.getTerminal().flush();
            while (true) {
                var line = reader.readLine(PROMPT);
                if ("exit".equals(line) || "quit".equals(line)) {
                    break;
                }
                var streamingResponse = chatService.sendAndStreamMessage(line, model);
                streamingResponse.toStream().forEach(chunk -> {
                    out.print(Ansi.AUTO.string(chunk));
                    out.flush();
                });
                out.println();
                reader.getTerminal().flush();
            }
        } else {
            var streamingResponse = chatService.sendAndStreamMessage(message, model);
            streamingResponse.toStream().forEach(chunk -> {
                out.print(Ansi.AUTO.string(chunk));
                out.flush();
            });
            out.println();
        }
    }
}
