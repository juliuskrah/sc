package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.jline.console.CommandRegistry;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sc.ai.cli.CliConfiguration;
import org.sc.ai.cli.chat.multimodal.ParsedPrompt;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatCommandTest {

    @Mock
    private ChatService chatService;
    Terminal terminal;
    LineReader lineReader;

    private StringWriter sw;
    private CommandLine cmd;
    private StreamingContext streamingContext;

    private CommandRegistry picocliCommands(Terminal terminal) {
        var chatSubCommand = new ChatSubCommand(terminal);
        var commandLine = new CommandLine(chatSubCommand);
        commandLine.addSubcommand("/exit", new ChatSubCommand.ExitCommand());
        return new PicocliCommands(commandLine);
    }

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.terminal();
        lineReader = spy(LineReaderBuilder.builder().terminal(terminal).build());
        sw = new StringWriter();
        var picocliCommands = picocliCommands(terminal);
        PrintWriter pw = new PrintWriter(sw);
        Supplier<Path> workDir = () -> Paths.get(".");
        SystemRegistry systemRegistry = new SystemRegistryImpl(
                new DefaultParser().regexCommand(CliConfiguration.REGEX_COMMAND), terminal, workDir, null);
        systemRegistry.setCommandRegistries(picocliCommands);
        systemRegistry.register("/?", picocliCommands);
        streamingContext = new StreamingContext();
        ChatCommand chatCommand = new ChatCommand(chatService, lineReader, systemRegistry, streamingContext);
        cmd = new CommandLine(chatCommand);
        cmd.setOut(pw);
        cmd.setErr(pw);
    }

    @Test
    @Disabled("Readline causes test to hang")
    void shouldEnterReplMode_whenNoMessageGiven() {
        // Given
        doReturn("exit").when(lineReader).readLine(anyString());
        when(chatService.sendAndStreamMessage(any(ParsedPrompt.class), anyString(), anyString()))
                .thenReturn(Flux.just("Mock response"));

        // When
        int exitCode = cmd.execute();

        // Then
        verify(lineReader, atMostOnce()).readLine(anyString());
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldUseSpecifiedModel_whenModelProvided() {
        // Given
        String specifiedModel = "customModel";
        when(chatService.sendAndStreamMessage(any(ParsedPrompt.class), eq(specifiedModel), anyString()))
                .thenReturn(Flux.just("Mock response"));

        // When
        String message = "test message";
        int exitCode = cmd.execute("-m", specifiedModel, message);

        // Then
        assertThat(sw).hasToString("Mock response\n");
        verify(chatService).sendAndStreamMessage(any(ParsedPrompt.class), eq(specifiedModel), anyString());
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldShowHelp_whenHelpOptionProvided() {
        // When
        int exitCode = cmd.execute("--help");

        // Then
        assertThat(sw.toString()).contains("Usage:", "chat");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldShowVersion_whenVersionOptionProvided() {
        // When
        int exitCode = cmd.execute("--version");

        // Then
        assertThat(sw.toString()).contains("sc", "JVM", "Build Time", "OS");
        assertThat(exitCode).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/bye", "/exit", "/quit" })
    void shouldExitReplMode_whenExitCommandGiven(String exitCommand) {
        // Given
        doReturn(exitCommand).when(lineReader).readLine(anyString());

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isZero();
        verify(lineReader).readLine("sc> ");
    }

    @Test
    void shouldCancelStreaming_whenInterrupted() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> {
            try {
                await().atMost(Duration.ofMillis(100)).until(() -> true);
            } catch (Exception _) {
                Thread.currentThread().interrupt();
            }
        });
        Flux<String> flux = Flux.interval(java.time.Duration.ofMillis(50))
                .map(i -> "chunk" + i)
                .take(5);
        when(chatService.sendAndStreamMessage(any(ParsedPrompt.class), isNull(), anyString())).thenReturn(flux);

        Thread t = new Thread(() -> cmd.execute("hello"));
        t.start();
        await().atMost(Duration.ofMillis(150)).until(future::isDone);
        streamingContext.cancel();
        t.join(1000);
        assertThat(sw.toString()).startsWith("chunk0");
        assertThat(sw.toString()).doesNotContain("chunk4");
    }

    @Test
    void shouldDisplaySpinner_whenWaitingForResponse() {
        // Simulate a delayed response that will trigger the spinner
        when(chatService.sendAndStreamMessage(any(ParsedPrompt.class), isNull(), anyString()))
                .thenReturn(Flux.just("Response").delayElements(java.time.Duration.ofMillis(200)));

        // When
        int exitCode = cmd.execute("test message");

        // Then
        assertThat(exitCode).isZero();
        String output = sw.toString();
        // The spinner should have cleared itself and the response should be visible
        assertThat(output).contains("Response");
        verify(chatService).sendAndStreamMessage(any(ParsedPrompt.class), isNull(), anyString());
    }
}
