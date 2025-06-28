package org.sc.ai.cli.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.UUID;
import org.jline.console.SystemRegistry;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.Widgets;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.stereotype.Component;
import org.sc.ai.cli.command.ChatbotVersionProvider;
import org.sc.ai.cli.command.ProviderMixin;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Component
@Command(name = "chat", description = "Chat with a bot", mixinStandardHelpOptions = true, versionProvider = ChatbotVersionProvider.class, subcommands = CommandLine.HelpCommand.class)
public class ChatCommand implements Runnable {
    private static final String PROMPT = "sc> ";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ChatCommand.class);
    private final ChatService chatService;
    private final OllamaChatProperties ollamaChatProperties;
    private final LineReader reader;
    private final SystemRegistry systemRegistry;
    @Parameters(arity = "0..1", paramLabel = "MESSAGE", description = "Message to send")
    private String message;
    @Option(names = { "-m",
            "--model" }, paramLabel = "MODEL", description = "Specify LLM to use")
    private String model;
    @Mixin
    private ProviderMixin ollamaMixin;
    @Spec
    private CommandLine.Model.CommandSpec spec;

    private String fromStdIn() throws IOException {
        if (System.in.available() == 0) {
            return null;
        }
        var messageBuilder = new StringBuilder();
        try (var bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                messageBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            logger.error("Encountered an error when reading from STDIN", e);
        }
        return messageBuilder.toString().trim();
    }

    private void streamModelResponse(String userMessage, String conversationId, PrintWriter writer) {
        model = Optional.ofNullable(model).orElse(ollamaChatProperties.getModel());
        logger.debug("LLM model: {}", model);
        var streamingResponse = chatService.sendAndStreamMessage(userMessage, model, conversationId);
        streamingResponse.toStream().forEach(chunk -> {
            writer.print(Ansi.AUTO.string(chunk));
            writer.flush();
        });
        writer.println();
    }

    public ChatCommand(ChatService chatService, LineReader reader, OllamaChatProperties ollamaChatProperties,
            SystemRegistry systemRegistry) {
        this.chatService = chatService;
        this.reader = reader;
        this.ollamaChatProperties = ollamaChatProperties;
        this.systemRegistry = systemRegistry;
    }

    @Override
    public void run() {
        loadMessageFromStdInIfNeeded();
        var conversationId = UUID.randomUUID().toString();
        
        if (message == null || message.isBlank()) {
            startInteractiveMode(conversationId);
        } else {
            streamModelResponse(message, conversationId, spec.commandLine().getOut());
        }
    }
    
    private void loadMessageFromStdInIfNeeded() {
        if (message == null) {
            try {
                message = fromStdIn();
            } catch (IOException e) {
                logger.warn("Error reading from stdin", e);
            }
        }
    }
    
    private void startInteractiveMode(String conversationId) {
        reader.getTerminal().flush();
        setupWidgetsAndKeyBindings();
        
        while (true) {
            try {
                if (!processUserInput(conversationId)) {
                    return;
                }
                reader.getTerminal().flush();
            } catch (UserInterruptException _) {
                // Ctrl-C pressed, ignore and continue
            } catch (EndOfFileException _) {
                // Ctrl-D pressed, exit
                return;
            } catch (Exception e) {
                systemRegistry.trace(e);
            }
        }
    }
    
    private void setupWidgetsAndKeyBindings() {
        TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5,
                TailTipWidgets.TipType.COMPLETER);
        widgets.enable();
        KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
        keyMap.bind(new Reference(Widgets.TAILTIP_TOGGLE), KeyMap.alt("s"));
    }
    
    private boolean processUserInput(String conversationId) throws Exception {
        systemRegistry.cleanUp();
        var line = reader.readLine(PROMPT).trim();
        
        if (line.isBlank()) {
            return true;
        }
        
        if ("exit".equals(line) || "quit".equals(line)) {
            return false;
        }
        
        if (line.startsWith("/") || "help".equals(line)) {
            systemRegistry.execute(line);
        } else {
            streamModelResponse(line, conversationId, reader.getTerminal().writer());
        }
        
        return true;
    }

}
