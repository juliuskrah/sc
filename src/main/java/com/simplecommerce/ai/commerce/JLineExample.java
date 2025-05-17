package com.simplecommerce.ai.commerce;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jline.console.CmdDesc;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.widget.AutopairWidgets;
import org.jline.widget.TailTipWidgets;

public class JLineExample {
    public static void main(String[] args) {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();) {
            terminal.handle(Terminal.Signal.INT, _ -> {
                terminal.writer().println("Use Ctrl + d or /quit to exit");
                terminal.writer().flush();
            });
            // Create a line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            var cmdDesc = new CmdDesc()
                .mainDesc(List.of(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                    .append(" [Command] - This is a test")
                    .toAttributedString()));
            var tailtipWidgets = new TailTipWidgets(reader, Map.of("father", cmdDesc), TailTipWidgets.TipType.COMBINED);
            var autopairWidgets = new AutopairWidgets(reader, true);

            autopairWidgets.enable();
            tailtipWidgets.enable();
            // Read lines from the user
            reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "type 'exit' to quit");
            while (true) {
                try {
                    if (Thread.interrupted()) {
                        break;
                    }
                    String line = reader.readLine("commerce> ");

                    // Exit if requested
                    if ("exit".equalsIgnoreCase(line)) {
                        break;
                    }

                    // Echo the line back to the user
                    terminal.writer().println("You entered: " + line);
                    terminal.flush();
                } catch (UserInterruptException _) {
                    Thread.currentThread().interrupt();
                }
            }

            terminal.writer().println("Goodbye!");
        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

}
