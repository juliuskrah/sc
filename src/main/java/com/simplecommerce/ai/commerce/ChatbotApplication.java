package com.simplecommerce.ai.commerce;

import org.jline.jansi.AnsiConsole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import picocli.CommandLine;

@SpringBootApplication
public class ChatbotApplication {

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = CommandLine.ExitCode.OK;
        try {
            exitCode = SpringApplication.exit(SpringApplication.run(ChatbotApplication.class, args));
        } finally {
            System.exit(exitCode);
            AnsiConsole.systemUninstall();
        }
    }

}
