package org.sc.ai.cli.rag;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class RagCommandTest {
    @Mock
    private RagService ragService;
    @InjectMocks
    private RagCommand ragCommand;

    @Test
    void shouldRequireDocument() {
        var cmd = new CommandLine(ragCommand);
        var writer = new StringWriter();
        cmd.setErr(new PrintWriter(writer));

        int exitCode = cmd.execute();
        assertThat(writer.toString()).contains("Missing required parameter: 'DOCUMENT'");
        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldValidateEtlTargetWithOutput() {
        var cmd = new CommandLine(ragCommand);
        var writer = new StringWriter();
        cmd.setErr(new PrintWriter(writer));

        int exitCode = cmd.execute("--etl=vectorStore", "--output=test.txt", "file:///test.pdf");
        assertThat(writer.toString()).contains("The --output option can only be used with '--etl=file'");
        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldRequireOutputWithFileEtl() {
        var cmd = new CommandLine(ragCommand);
        var writer = new StringWriter();
        cmd.setErr(new PrintWriter(writer));

        int exitCode = cmd.execute("--etl=file", "file:///test.txt");
        assertThat(writer.toString()).contains("The --output option is required when using '--etl=file'");
        assertThat(exitCode).isNotZero();
    }

    @Test
    void shouldAcceptValidFileInput() {
        var cmd = new CommandLine(ragCommand);
        var writer = new StringWriter();
        cmd.setErr(new PrintWriter(writer));

        int exitCode = cmd.execute("file:///test.txt");
        assertThat(writer.toString()).contains("The --output option is required when using '--etl=file'");
        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void shouldAcceptOutputWithFileEtl() throws IOException {
        var cmd = new CommandLine(ragCommand);
        var writer = new StringWriter();
        cmd.setErr(new PrintWriter(writer));

        int exitCode = cmd.execute("--etl=file", "--output=output.txt", "file:///test.txt");
        assertThat(writer.toString()).isEmpty();
        assertThat(exitCode).isZero();
        verify(ragService).processToFile("file:///test.txt", Path.of("output.txt"));
    }
}
