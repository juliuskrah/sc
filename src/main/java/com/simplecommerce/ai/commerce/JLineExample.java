package com.simplecommerce.ai.commerce;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class JLineExample {
    public static void main(String[] args) {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();) {

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Read lines from the user
            while (true) {
                String line = reader.readLine("commerce> ");

                // Exit if requested
                if ("exit".equalsIgnoreCase(line)) {
                    break;
                }

                // Echo the line back to the user
                terminal.writer().println("You entered: " + line);
                terminal.flush();
            }

            terminal.writer().println("Goodbye!");
            // The terminal is automatically closed by the try-with-resources statement

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

}
