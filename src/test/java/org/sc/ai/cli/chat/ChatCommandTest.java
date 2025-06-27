package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;

import picocli.CommandLine;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatCommandTest {

    @Mock
    private ChatService chatService;

    @Mock
    private LineReader lineReader;

    @Mock
    private OllamaChatProperties ollamaChatProperties;

    private StringWriter sw;
    private PrintWriter pw;
    private ChatCommand chatCommand;
    private CommandLine cmd;

    @BeforeEach
    void setUp() {
        sw = new StringWriter();
        pw = new PrintWriter(sw);
        
        chatCommand = new ChatCommand(chatService, lineReader, ollamaChatProperties);
        cmd = new CommandLine(chatCommand);
        cmd.setOut(pw);
        cmd.setErr(pw);
    }

    @Test
    void shouldUseDefaultModel_whenModelNotSpecified() {
        // Given
        String message = "send this message";
        String defaultModel = "llama2";
        when(ollamaChatProperties.getModel()).thenReturn(defaultModel);
        when(chatService.sendAndStreamMessage(any(), any(), any()))
            .thenReturn(Flux.just("Mock response"));

        // When
        int exitCode = cmd.execute(message);

        // Then
        assertThat(sw).hasToString("Mock response\n");
        verify(chatService).sendAndStreamMessage(anyString(), anyString(), anyString());
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldUseSpecifiedModel_whenModelProvided() {
        // Given
        String message = "Hello";
        String model = "mistral";
        when(chatService.sendAndStreamMessage(any(), any(), any()))
            .thenReturn(Flux.just("Another mock response"));

        // When
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

    @Test
    void shouldExitInteractiveMode_whenExitCommandGiven() {
        // Given
        var terminal = mock(Terminal.class);
        when(lineReader.readLine("sc> ")).thenReturn("exit");
        when(lineReader.getTerminal()).thenReturn(terminal);

        // When
        int exitCode = cmd.execute();

        // Then
        verify(lineReader).readLine("sc> ");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldExitInteractiveMode_whenQuitCommandGiven() {
        // Given
        var terminal = mock(Terminal.class);
        when(lineReader.getTerminal()).thenReturn(terminal);
        when(lineReader.readLine("sc> ")).thenReturn("quit");

        // When
        int exitCode = cmd.execute();

        // Then
        verify(lineReader).readLine("sc> ");
        assertThat(exitCode).isZero();
    }
}
