package org.sc.ai.cli;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.PathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.ollama.OllamaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class AiTestConfiguration {
    @Value("${OLLAMA_MODELS:${user.home}/.ollama}")
    private PathResource ollamaPathResource;

    @Bean
    @ServiceConnection
    OllamaContainer ollamaContainer() {
        return new OllamaContainer("ollama/ollama")
            .withFileSystemBind(ollamaPathResource.getPath(), "/root/.ollama", BindMode.READ_WRITE);
    }

    @Bean
    ApplicationRunner ollamaInitializer(OllamaContainer ollama) {
        return _ -> ollama.execInContainer("ollama", "pull", "snowflake-arctic-embed:22m");
    }
}
