package org.sc.ai.cli.chat;

import java.io.PrintWriter;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Component
@Command(name = "", description = {
                "Hit @|fg(yellow) <TAB>|@ to see available commands",
                "Hit @|fg(yellow) ALT+S|@ to toggle tailtips",
                "" }, footer = { "@|fg(240) Use|@ @|fg(yellow) \"\"\"|@ @|fg(240) to begin a multi-line message.|@",
                                "@|fg(240) Use|@ @|fg(yellow),italic @/path/to/file|@ @|fg(240) to include|@ @|fg(240),bold .jpg, .png,|@ @|fg(240) or|@ @|fg(240),bold .webp|@ @|fg(240) images.|@",
                })
public class ChatSubCommand implements Runnable {
        final PrintWriter out;
        final Terminal terminal;
        @Spec
        private CommandLine.Model.CommandSpec spec;

        public ChatSubCommand(Terminal terminal) {
                this.terminal = terminal;
                this.out = terminal.writer();
        }

        @Override
        public void run() {
                out.println(spec.commandLine().getUsageMessage());
        }

        @Command(name = "", mixinStandardHelpOptions = true, description = { "Set session variables" })
        public static class SetCommand implements Runnable {
                @ParentCommand
                ChatSubCommand parent;
                @Spec
                private CommandLine.Model.CommandSpec spec;

                @Override
                public void run() {
                        parent.out.println("/set command executed.");
                }
        }

        @Command(name = "", mixinStandardHelpOptions = true, description = { "Show model information" })
        public static class ShowCommand implements Runnable {

                @ParentCommand
                ChatSubCommand parent;

                @Override
                public void run() {
                        parent.out.println("/show command executed.");
                }
        }

        @Command(name = "", mixinStandardHelpOptions = true, description = { "Exit" })
        public static class ExitCommand implements Runnable {
                @ParentCommand
                ChatSubCommand parent;

                @Override
                public void run() {
                        parent.out.println("Goodbye!");
                        parent.out.flush();
                }
        }

        @Command(name = "", mixinStandardHelpOptions = true, description = { "Clears the screen" })
        public static class ClearScreen implements Runnable {
                @ParentCommand
                ChatSubCommand parent;

                @Override
                public void run() {
                        parent.terminal.puts(Capability.clear_screen);
                }
        }
}
