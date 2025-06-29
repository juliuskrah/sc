package org.sc.ai.cli;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ollama.OllamaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class AiTestConfiguration {

    @Bean
    @ServiceConnection
    OllamaContainer ollamaContainer() {
        return new OllamaContainer("ollama/ollama");
    }
}
