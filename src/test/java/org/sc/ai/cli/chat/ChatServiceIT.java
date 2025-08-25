package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.sc.ai.cli.AiTestConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import reactor.core.publisher.Flux;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"sc.vector.simple.store=${java.io.tmpdir}/vector-store"})
@ContextConfiguration(classes = { AiTestConfiguration.class, ChatServiceIT.TestConfig.class })
class ChatServiceIT {
    @Value("${sc.vector.simple.store}")
    private PathResource vectorStoreStorageDirectory;

    @BeforeEach
    void setUp() throws IOException {
        var path = Paths.get(vectorStoreStorageDirectory.getURI());
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        Files.write(path.resolve("vector.json"), 
        new ClassPathResource("/vectors/1753699946506.json").getInputStream().readAllBytes());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        @Primary
        ChatClient.Builder chatClientBuilder() {
            ChatClient mockClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.StreamResponseSpec mockStreamSpec = mock(ChatClient.StreamResponseSpec.class);

            when(mockClient.prompt()).thenReturn(mockRequestSpec);
            when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
            when(mockRequestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())).thenReturn(mockRequestSpec);
            when(mockRequestSpec.options(any())).thenReturn(mockRequestSpec);
            when(mockRequestSpec.stream()).thenReturn(mockStreamSpec);
            when(mockStreamSpec.content()).thenReturn(Flux.just("Mock response"));
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            when(mockBuilder.defaultAdvisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
            return mockBuilder;
        }
    }

    @MockitoBean
    private ChatMemory chatMemory;

    @Autowired
    private ChatService chatService;

    @Test
    void sendAndStreamMessage_shouldStreamValidResponse() {
        // Given
        String message = "Hello";
        String model = "llama2";

        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);

        // Then
        assertThat(result.blockFirst()).isEqualTo("Mock response");
    }

    @Test
    void sendAndStreamMessage_shouldThrowException_whenMessageIsEmpty() {
        // Given
        String message = "";
        String model = "llama2";

        // When/Then
        assertThatThrownBy(() -> chatService.sendAndStreamMessage(message, model, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message must not be empty");
    }

    @Test
    void sendAndStreamMessage_shouldThrowException_whenModelIsEmpty() {
        // Given
        String message = "Hello";
        String model = "";

        // When/Then
        assertThatCode(() -> chatService.sendAndStreamMessage(message, model, null))
                .doesNotThrowAnyException();
    }
}