package org.sc.ai.cli.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
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
    private final VectorStore vectorStore;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    @Value("${sc.vector.simple.store:}")
    private PathResource vectorStoreStorageDirectory;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, EmbeddingModel embeddingModel) {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(advisors -> advisors.advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).scheduler(BaseAdvisor.DEFAULT_SCHEDULER).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()))
                .build();
    }

    public Flux<String> sendAndStreamMessage(String message, String model, @Nullable String conversationId) {
        Assert.hasText(message, "Message must not be empty");
        Assert.hasText(model, "Model must not be empty");
        logger.info("Sending message: \"{}\" using model: {}", message, model);
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            try (var files = Files.walk(vectorStoreStorageDirectory.getFile().toPath(), 1)
                    .filter(Files::isRegularFile)) {
                files.forEach(path -> simpleVectorStore.load(new PathResource(path)));
            } catch (IOException e) {
                logger.error("Failed to load vector store files", e);
            } 
        }
        var options = OllamaOptions.builder()
                .model(model)
                .build();
        return chatClient.prompt().user(message).options(options)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID,
                        Optional.ofNullable(conversationId).orElse(UUID.randomUUID().toString())))
                .stream().content();

    }

}
