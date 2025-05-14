package com.simplecommerce.ai.commerce;

import org.jline.jansi.AnsiConsole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.simplecommerce.ai.commerce.command.CommerceCommand;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.shell.jline3.PicocliCommands;

@SpringBootApplication
public class CommerceAiApplication implements CommandLineRunner, ExitCodeGenerator {
    private final IFactory factory;
    private final CommerceCommand command;
    private int exitCode;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = 0;
        try {
            exitCode = SpringApplication.exit(SpringApplication.run(CommerceAiApplication.class, args));
        } finally{
            System.exit(exitCode);
            AnsiConsole.systemUninstall();
        }
    }

    CommerceAiApplication(IFactory factory, CommerceCommand command) {
        this.factory = factory;
        this.command = command;
    }

    @Override
    public void run(String... args) {
        // Supplier<Path> workDir = () -> {
        //     var path = System.getProperty("user.dir");
        //     return path != null ? Paths.get(path) : Paths.get(".");
        // };
        // var builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
        // builtins.rename(Builtins.Command.TTOP, "top");
        // builtins.alias("zle", "widget");
        // builtins.alias("bindkey", "keymap");

        var commandsFactory = new PicocliCommands.PicocliCommandsFactory(factory);
        var cmd = new CommandLine(command, commandsFactory);
        // var picocliCommands = new PicocliCommands(cmd);
        exitCode = cmd.execute(args);
        // var parser = new DefaultParser();
        // try(var terminal = TerminalBuilder.builder().build()) {
        //     var systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
        //     systemRegistry.setCommandRegistries(builtins, picocliCommands);
        //     systemRegistry.register("help", picocliCommands);

        //     var lineReader = LineReaderBuilder.builder()
        //             .terminal(terminal)
        //             .completer(systemRegistry.completer())
        //             .parser(parser)
        //             .variable(LineReader.LIST_MAX, 50)
        //             .build();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }

    @Override 
    public int getExitCode() {
        return exitCode;
    }
}
