package com.simplecommerce.ai.commerce;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Julius Krah
 */
@Configuration(proxyBeanMethods = false)
public class CliConfiguration {
    /**
     * Creates a {@link Terminal} bean that is used by JLine to read and write to the console.
     * 
     * @return a {@link Terminal} instance
     * @throws IOException if an I/O error occurs
     */
    @Bean
    Terminal terminal() throws IOException {
        var terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        terminal.handle(Terminal.Signal.INT, signal -> {
            terminal.writer().println("Received SIG" + signal + " (CTR+C)");
            terminal.flush();
        });
        return terminal;
    }

    @Bean
    LineReader lineReader(Terminal terminal, @Value("${spring.application.name}") String appName) {
        return LineReaderBuilder.builder()
            .terminal(terminal)
            .option(LineReader.Option.AUTO_FRESH_LINE, true)
            .option(LineReader.Option.HISTORY_BEEP, true)
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "type 'exit' to 'quit'")
            .appName(appName)
            .build();
    }

}
