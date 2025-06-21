package org.simplecommerce.ai.commerce.config;

import java.util.List;
import java.util.Map;

import org.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import org.simplecommerce.ai.commerce.command.ProviderMixin;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * @author Julius Krah
 */
@Component
@Command(name = "config", description = "Manage configuration settings", mixinStandardHelpOptions = true, versionProvider = ChatbotVersionProvider.class, subcommands = CommandLine.HelpCommand.class)
public class ConfigCommand implements Runnable {
    private final ConfigService configService;
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private ProviderMixin ollamaMixin;
    @ArgGroup(exclusive = true, multiplicity = "0..1")
    private Options options;

    public ConfigCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void run() {
        if (options == null) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }
        if (options.dir) {
            String dir = configService.getDir();
            if (dir != null) {
                spec.commandLine().getOut().println(dir);
            } else {
                spec.commandLine().getOut().println(Ansi.AUTO.string("""
                Configuration directory does not exist, you can use any of the following options to create it:

                    @|bold sc config init|@                  Initialize the configuration file
                    @|bold sc config|@ @|fg(yellow) --set|@=@|italic KEY=VALUE|@       Set configuration key-value pairs
                """));
            }
        }
        if (options.file) {
            String file = configService.getFilePath();
            if (file != null) {
                spec.commandLine().getOut().println(file);
            } else {
                spec.commandLine().getOut().println(Ansi.AUTO.string("""
                Configuration file does not exist, you can use any of the following options to create a config file:

                    @|bold sc config init|@                  Initialize the configuration file
                    @|bold sc config|@ @|fg(yellow) --set|@=@|italic KEY=VALUE|@       Set configuration key-value pairs
                """));
            }
        }
        if (options.get != null) {
            spec.commandLine().getOut().println(configService.get(options.get));
        }
        if (options.set != null) {
            try {
                configService.set(options.set);
                spec.commandLine().getOut().println(options.set);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                spec.commandLine().getErr().println("Failed to set configuration: " + ex.getMessage());
            }
        }
        if (options.unset != null) {
            configService.unset(options.unset);
            spec.commandLine().getOut().println(options.unset);
        }
    }

    @Command(name = "init", description = "Initialize the configuration file", mixinStandardHelpOptions = true, versionProvider = ChatbotVersionProvider.class, subcommands = CommandLine.HelpCommand.class)
    public void init() {
        configService.init();
        spec.commandLine().getOut().println("Configuration file initialized.");
    }

    static class Options {
        @Option(names = "--dir", paramLabel = "DIRECTORY", description = "Display the configuration directory")
        private boolean dir;
        @Option(names = "--file", paramLabel = "FILE", description = "Display the configuration file")
        private boolean file;
        @Option(names = "--set", paramLabel = "KEY=VALUE", description = "Set configuration key-value pairs")
        private Map<String, String> set;
        @Option(names = "--get", paramLabel = "KEY", description = "Get a configuration value by key")
        private String get;
        @Option(names = "--unset", paramLabel = "KEY", description = "Unset a configuration properties")
        private List<String> unset;
    }
}
