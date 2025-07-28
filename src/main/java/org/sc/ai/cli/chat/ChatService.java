package org.sc.ai.cli.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.sc.ai.cli.chat.multimodal.ParsedPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

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
        return chatClient.prompt().user(message)
                .options(options)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID,
                        Optional.ofNullable(conversationId).orElse(UUID.randomUUID().toString())))
                .stream().content();
    }

    /**
     * Sends a parsed prompt (with potential file attachments) and streams the response.
     * 
     * @param parsedPrompt the parsed prompt containing text and file paths
     * @param model the model to use
     * @param conversationId the conversation ID (optional)
     * @return a Flux of response content
     */
    public Flux<String> sendAndStreamMessage(ParsedPrompt parsedPrompt, String model, @Nullable String conversationId) {
        if (!parsedPrompt.hasFiles()) {
            // No files, use the simple text-only method
            return sendAndStreamMessage(parsedPrompt.textContent(), model, conversationId);
        }

        // Handle multimodal prompt with files
        logger.info("Processing multimodal prompt with {} file(s)", parsedPrompt.fileCount());
        
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            if (!vectorStoreStorageDirectory.exists()) {
                logger.error("Vector store storage directory does not exist: {}", vectorStoreStorageDirectory);
            } else {
                try (var files = Files.walk(vectorStoreStorageDirectory.getFile().toPath(), 1)
                        .filter(Files::isRegularFile)) {
                    files.forEach(path -> simpleVectorStore.load(new PathResource(path)));
                } catch (IOException e) {
                    logger.error("Failed to load vector store files", e);
                } 
            }
        }
        
        var options = OllamaOptions.builder()
                .model(model)
                .build();
                
        // Convert file paths to Media objects
        var mediaObjects = parsedPrompt.filePaths().stream()
                .map(this::createMediaFromPath)
                .filter(Objects::nonNull)
                .toArray(Media[]::new);
        
        return chatClient.prompt().user(u -> u.text(parsedPrompt.textContent()).media(mediaObjects))
                .options(options)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID,
                        Optional.ofNullable(conversationId).orElse(UUID.randomUUID().toString())))
                .stream().content();
    }
    
    /**
     * Creates a Media object from a file path with appropriate MIME type detection.
     * 
     * @param filePath the path to the media file
     * @return Media object or null if the file cannot be processed
     */
    private Media createMediaFromPath(Path filePath) {
        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                logger.warn("File does not exist or is not a regular file: {}", filePath);
                return null;
            }
            
            String fileName = filePath.getFileName().toString().toLowerCase();
            MimeType mimeType;
            
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            } else if (fileName.endsWith(".png")) {
                mimeType = MimeTypeUtils.IMAGE_PNG;
            } else if (fileName.endsWith(".gif")) {
                mimeType = MimeTypeUtils.IMAGE_GIF;
            } else if (fileName.endsWith(".webp")) {
                mimeType = MimeType.valueOf("image/webp");
            } else if (fileName.endsWith(".bmp")) {
                mimeType = MimeType.valueOf("image/bmp");
            } else {
                logger.warn("Unsupported image format for file: {}", filePath);
                return null;
            }
            
            return new Media(mimeType, new PathResource(filePath));
            
        } catch (Exception e) {
            logger.error("Failed to create media object from path: {}", filePath, e);
            return null;
        }
    }

}
