package org.sc.ai.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.jline.console.CommandRegistry;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.sc.ai.cli.chat.ChatSubCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.shell.jline3.PicocliCommands;

/**
 * @author Julius Krah
 */
@Configuration(proxyBeanMethods = false)
public class CliConfiguration {
    final Parser parser = new DefaultParser();
    final Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

    /**
     * Creates a {@link Terminal} bean that is used by JLine to read and write to
     * the console.
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
        terminal.handle(Terminal.Signal.TSTP, signal -> {
            terminal.writer().println("Received SIG" + signal + " (CTR+Z)");
            terminal.flush();
        });
        return terminal;
    }

    @Bean
    LineReader lineReader(Terminal terminal, SystemRegistry systemRegistry, @Value("${spring.application.name}") String appName) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, true)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "type 'exit' to quit")
                .variable(LineReader.LIST_MAX, 50)
                .appName(appName)
                .build();
    }

    @Bean
    SystemRegistry systemRegistry(Terminal terminal, IFactory factory, ChatSubCommand chatSubCommands) {
        var picocliCommands = picocliCommands(terminal, factory, chatSubCommands);
        SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
        systemRegistry.setCommandRegistries(picocliCommands);
        systemRegistry.register("/?", picocliCommands);
        return systemRegistry;
    }

    private CommandRegistry picocliCommands(Terminal terminal, IFactory factory, ChatSubCommand chatSubCommands) {
        CommandLine cmd = new CommandLine(chatSubCommands, picocliCommandsFactory(terminal, factory));
        cmd.addSubcommand("/set", new ChatSubCommand.SetCommand());
        cmd.addSubcommand("/show", new ChatSubCommand.ShowCommand());
        cmd.addSubcommand("/clear", new ChatSubCommand.ClearScreen(), "/cls");
        cmd.addSubcommand("/bye", new ChatSubCommand.ExitCommand(), "/exit", "/quit");
        cmd.addSubcommand("/help", new CommandLine.HelpCommand(), "/?");
        return new PicocliCommands(cmd);
    }

    private PicocliCommands.PicocliCommandsFactory picocliCommandsFactory(Terminal terminal, IFactory factory) {
        var picocliCommandsFactory = new PicocliCommands.PicocliCommandsFactory(factory);
        picocliCommandsFactory.setTerminal(terminal);
        return picocliCommandsFactory;
    }

}
