package com.simplecommerce.ai.commerce;

import org.jline.jansi.AnsiConsole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class ChatbotApplication {

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        try {
            int exitCode = SpringApplication.exit(SpringApplication.run(ChatbotApplication.class, args));
            System.exit(exitCode);
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

}
