package org.simplecommerce.ai.commerce.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import reactor.core.publisher.Flux;

@ActiveProfiles("test")
@SpringBootTest(classes = {ChatService.class, ChatServiceTest.TestConfig.class})
class ChatServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ChatClient.Builder chatClientBuilder() {
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
        assertThatThrownBy(() -> chatService.sendAndStreamMessage(message, model, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Model must not be empty");
    }
}