package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sc.ai.cli.CliConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;

import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatCommandTest {

    @Mock
    private ChatService chatService;
    @Mock
    private OllamaChatProperties ollamaChatProperties;
    Terminal terminal;
    @Spy
    LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();

    private StringWriter sw;
    private CommandLine cmd;

    private CommandRegistry picocliCommands(Terminal terminal) {
        var chatSubCommand = new ChatSubCommand(terminal);
        var commandLine = new CommandLine(chatSubCommand);
        commandLine.addSubcommand("/exit", new ChatSubCommand.ExitCommand());
        return new PicocliCommands(commandLine);
    }

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.terminal();
        sw = new StringWriter();
        var picocliCommands = picocliCommands(terminal);
        PrintWriter pw = new PrintWriter(sw);
        Supplier<Path> workDir = () -> Paths.get(".");
        SystemRegistry systemRegistry = new SystemRegistryImpl(new DefaultParser().regexCommand(CliConfiguration.REGEX_COMMAND), terminal, workDir, null);
        systemRegistry.setCommandRegistries(picocliCommands);
        systemRegistry.register("/?", picocliCommands);
        ChatCommand chatCommand = new ChatCommand(chatService, lineReader, ollamaChatProperties, systemRegistry);
        cmd = new CommandLine(chatCommand);
        cmd.setOut(pw);
        cmd.setErr(pw);
    }

    @Test
    void shouldUseDefaultModel_whenModelNotSpecified() {
        // Given
        String defaultModel = "llama2";
        when(ollamaChatProperties.getModel()).thenReturn(defaultModel);
        when(chatService.sendAndStreamMessage(any(), any(), any()))
                .thenReturn(Flux.just("Mock response"));

        // When
        String message = "send this message";
        int exitCode = cmd.execute(message);

        // Then
        assertThat(sw).hasToString("Mock response\n");
        verify(chatService).sendAndStreamMessage(anyString(), anyString(), anyString());
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldEnterReplMode_whenNoMessageGiven() {
        // Given
        doReturn("What is the meaning of life?", "/exit").when(lineReader).readLine(anyString());
        when(chatService.sendAndStreamMessage(any(), any(), any()))
                .thenReturn(Flux.just("<ignored>"));

        // When
        String model = "llama2";
        int exitCode = cmd.execute("-m", model);

        // Then
        verify(chatService, atMostOnce()).sendAndStreamMessage(anyString(), anyString(), anyString());
        verify(lineReader, times(2)).readLine("sc> ");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldUseSpecifiedModel_whenModelProvided() {
        // Given
        when(chatService.sendAndStreamMessage(any(), any(), any()))
                .thenReturn(Flux.just("Another mock response"));

        // When
        String model = "mistral";
        String message = "Hello";
        int exitCode = cmd.execute(message, "-m", model);

        // Then
        assertThat(sw).hasToString("Another mock response\n");
        verify(chatService).sendAndStreamMessage(anyString(), anyString(), anyString());
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
    @ValueSource(strings = {"/bye", "/exit", "/quit"})
    void shouldExitReplMode_whenExitCommandGiven(String exitCommand) {
        // Given
        doReturn(exitCommand).when(lineReader).readLine(anyString());

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isZero();
        verify(lineReader).readLine("sc> ");
    }
}
