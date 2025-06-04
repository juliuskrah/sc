package org.simplecommerce.ai.commerce.chat;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;

/**
 * @author Julius Krah
 */
@Service
public class ChatService {
    private final ChatClient chatClient;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(advisors -> advisors.advisors(MessageChatMemoryAdvisor.builder(chatMemory).scheduler(BaseAdvisor.DEFAULT_SCHEDULER).build()))
                .build();
    }

    public Flux<String> sendAndStreamMessage(String message, String model, @Nullable String conversationId) {
        Assert.hasText(message, "Message must not be empty");
        Assert.hasText(model, "Model must not be empty");
        logger.info("Sending message: \"{}\" using model: {}", message, model);
        var options = OllamaOptions.builder()
                .model(model)
                .build();
        return chatClient.prompt().user(message).options(options)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, Optional.ofNullable(conversationId).orElse(UUID.randomUUID().toString())))
                .stream().content();

    }

}
