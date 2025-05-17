package com.simplecommerce.ai.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class ChatbotApplication {

    public static void main(String[] args) {
        int exitCode = SpringApplication.exit(SpringApplication.run(ChatbotApplication.class, args));
        System.exit(exitCode);
    }

}
