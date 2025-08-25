package org.sc.ai.cli.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.jline.console.SystemRegistry;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.Widgets;
import org.sc.ai.cli.chat.multimodal.PromptParser;
import org.springframework.stereotype.Component;
import org.sc.ai.cli.command.ChatbotVersionProvider;
import org.sc.ai.cli.command.ProviderMixin;
import org.sc.ai.cli.command.Spinner;
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
    private final LineReader reader;
    private final SystemRegistry systemRegistry;
    private final StreamingContext streamingContext;
    private final PromptParser promptParser = new PromptParser();
    @Parameters(arity = "0..1", paramLabel = "MESSAGE", description = "Message to send")
    private String message;
    @Option(names = { "-m", "--model" }, paramLabel = "MODEL", description = "Specify LLM to use")
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
        if (model != null && !model.isBlank()) {
            logger.debug("Using '{}' model", model);
        }

        // Parse the message for potential file attachments
        var parsedPrompt = promptParser.parse(userMessage);
        
        // Start spinner to indicate processing (delayed start)
        var spinner = new Spinner(writer, "Thinking...");
        spinner.start();
        
        var streamingResponse = chatService.sendAndStreamMessage(parsedPrompt, model, conversationId);
        var latch = new CountDownLatch(1);
        
        var disposable = streamingResponse.subscribe(chunk -> {
            spinner.stop();
            writer.print(Ansi.AUTO.string(chunk));
            writer.flush();
        }, error -> {
            spinner.stop();
            logger.error("Error streaming response", error);
            writer.println();
            latch.countDown();
        }, () -> {
            spinner.stop();
            writer.println();
            latch.countDown();
        });
        streamingContext.register(disposable, latch, spinner);
        try {
            latch.await();
        } catch (InterruptedException _) {
            spinner.stop();
            Thread.currentThread().interrupt();
        } finally {
            streamingContext.clear();
        }
    }

    public ChatCommand(ChatService chatService, LineReader reader,
            SystemRegistry systemRegistry, StreamingContext streamingContext) {
        this.chatService = chatService;
        this.reader = reader;
        this.systemRegistry = systemRegistry;
        this.streamingContext = streamingContext;
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
                reader.getTerminal().writer().println("Use Ctrl + d or /bye to exit.");
                reader.getTerminal().flush();
            } catch (EndOfFileException _) {
                // Ctrl-D pressed, exit gracefully
                return;
            } catch (Exception e) {
                systemRegistry.trace(e);
            }
        }
    }

    private void setupWidgetsAndKeyBindings() {
        final String INSERT_DATE = "insert-date";
        Widget insertDateWidget = this::insertDateWidget;

        TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5,
                TailTipWidgets.TipType.COMPLETER);
        widgets.addWidget(INSERT_DATE, insertDateWidget);

        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        keyMap.bind(new Reference(INSERT_DATE), KeyMap.ctrl('Q'));
        Binding currentBinding = keyMap.getBound(KeyMap.alt('S'));
        if (currentBinding == null) {
            keyMap.bind(new Reference(Widgets.TAILTIP_TOGGLE), KeyMap.alt('S'));
        } else {
            keyMap.bind(new Reference(Widgets.TAILTIP_TOGGLE), KeyMap.ctrl('S'));
        }
    }

    boolean insertDateWidget() {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        reader.getBuffer().write(date);
        return true;
    }

    private boolean processUserInput(String conversationId) throws Exception {
        systemRegistry.cleanUp();
        var line = reader.readLine(PROMPT).trim();

        if (line.isBlank()) {
            return true;
        }

        if (line.startsWith("/")) {
            // Handle custom exit commands before passing to SystemRegistry
            if ("/bye".equals(line) || "/exit".equals(line) || "/quit".equals(line)) {
                reader.getTerminal().writer().println("Goodbye!");
                reader.getTerminal().flush();
                return false;
            }
            systemRegistry.execute(line);
        } else {
            streamModelResponse(line, conversationId, reader.getTerminal().writer());
        }

        return true;
    }

}
