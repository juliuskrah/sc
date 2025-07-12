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
import org.sc.ai.cli.chat.StreamingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.shell.jline3.PicocliCommands;

/**
 * @author Julius Krah
 */
@Configuration(proxyBeanMethods = false)
public class CliConfiguration {
    @Value("${sc.config.dir:}")
    private PathResource configDirectory;
    public static final String REGEX_COMMAND = "([/:]?[a-zA-Z]+[a-zA-Z0-9_-]*|[/?]|\\?|/\\?)";
    final Parser parser = new DefaultParser().regexCommand(REGEX_COMMAND).eofOnUnclosedQuote(true);
    final Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

    /**
     * Creates a {@link Terminal} bean that is used by JLine to read and write to
     * the console.
     * 
     * @return a {@link Terminal} instance
     * @throws IOException if an I/O error occurs
     */
    @Bean
    Terminal terminal(@Value("${spring.application.name}") String name, StreamingContext streamingContext) throws IOException {
        var terminal = TerminalBuilder.builder()
                .name(name)
                .system(true)
                .build();
        terminal.handle(Terminal.Signal.INT, signal -> {
            streamingContext.cancel();
            terminal.writer().println("Generation cancelled.");
            terminal.flush();
        });
        terminal.handle(Terminal.Signal.TSTP, signal -> {
            terminal.writer().println("Received SIG" + signal + " (CTR+Z)");
            terminal.flush();
        });
        return terminal;
    }

    @Bean
    LineReader lineReader(Terminal terminal, SystemRegistry systemRegistry,
            @Value("${spring.application.name}") String appName) throws IOException {
        Path historyFile = Paths.get(configDirectory.getURI()).resolve("history");
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .variable(LineReader.HISTORY_FILE, historyFile)
                .variable(LineReader.HISTORY_SIZE, 100)
                .variable(LineReader.HISTORY_FILE_SIZE, 2000)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%{...%} ")
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
