package org.simplecommerce.ai.commerce.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;

/**
 * @author Julius Krah
 */
@Service
public class ChatService {
    private final ChatClient chatClient;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatClient.Builder chatClientBuilder) {

        this.chatClient = chatClientBuilder.build();
    }

    public Flux<String> sendAndStreamMessage(String message, String model) {
        Assert.hasText(message, "Message must not be empty");
        Assert.hasText(model, "Model must not be empty");
        logger.info("Sending message: \"{}\" using model: {}", message, model);
        var options = OllamaOptions.builder()
                .model(model)
                .build();
        return chatClient.prompt().user(message).options(options).stream().content();

    }
    
}
