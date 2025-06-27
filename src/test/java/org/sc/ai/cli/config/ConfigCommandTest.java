package org.sc.ai.cli.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class ConfigCommandTest {
    @Mock
    private ConfigService configService;
    @InjectMocks
    private ConfigCommand configCommand;

    @Test
    void shouldPrintConfigDirectoryInstructions() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));

        int exitCode = cmd.execute("--dir");
        assertThat(writer)
                .hasToString("""
                        Configuration directory does not exist, you can use any of the following options to create it:

                            sc config init                  Initialize the configuration file
                            sc config --set=KEY=VALUE       Set configuration key-value pairs

                        """);
        assertThat(exitCode).isZero();
    }


    @Test
    void shouldPrintConfigDirectory() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));
        when(configService.getDir()).thenReturn("/path/to/config/dir"); 

        int exitCode = cmd.execute("--dir");
        assertThat(writer).hasToString("/path/to/config/dir\n");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldPrintConfigFileInstructions() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));

        int exitCode = cmd.execute("--file");
        assertThat(writer)
                .hasToString("""
                        Configuration file does not exist, you can use any of the following options to create a config file:

                            sc config init                  Initialize the configuration file
                            sc config --set=KEY=VALUE       Set configuration key-value pairs

                        """);
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldPrintConfigFile() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));
        when(configService.getFilePath()).thenReturn("/path/to/config/file");

        int exitCode = cmd.execute("--file");
        assertThat(writer).hasToString("/path/to/config/file\n");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldShowUsageIfNoOptions() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));

        int exitCode = cmd.execute();
        assertThat(writer.toString()).contains("Usage:");
        assertThat(exitCode).isZero();
    }

    @Test
    void shouldCallInitSubcommand() {
        var cmd = new CommandLine(configCommand);
        var writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));

        int exitCode = cmd.execute("init");
        assertThat(writer.toString()).contains("Configuration file initialized.");
        assertThat(exitCode).isZero();
    }
}