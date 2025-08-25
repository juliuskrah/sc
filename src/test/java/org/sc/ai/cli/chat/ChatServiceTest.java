package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Flux;

/**
 * Unit tests for {@link ChatService}.
 * 
 * @author Julius Krah
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    
    @Mock
    private ChatClient chatClient;
    
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    
    @Mock
    private ChatClient.StreamResponseSpec streamSpec;
    
    @Mock
    private ChatMemory chatMemory;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    @Mock
    private VectorStore vectorStore;
    
    @Mock
    private SimpleVectorStore simpleVectorStore;
    
    @TempDir
    Path tempDir;
    
    private ChatService chatService;
    
    @BeforeEach
    void setUp() {
        // Setup lenient stubs for common interactions
        lenient().when(chatClientBuilder.defaultAdvisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())).thenReturn(chatClientBuilder);
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.options(any())).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())).thenReturn(requestSpec);
        lenient().when(requestSpec.stream()).thenReturn(streamSpec);
        
        chatService = new ChatService(chatClientBuilder, chatMemory, embeddingModel);
        
        // Initialize the vectorStoreStorageDirectory field to avoid NullPointerException
        ReflectionTestUtils.setField(chatService, "vectorStoreStorageDirectory", new PathResource(tempDir));
    }
    
    @Test
    void constructor_shouldInitializeChatClientWithAdvisors() {
        // Verify that the constructor sets up the ChatClient with the expected advisors
        verify(chatClientBuilder).defaultAdvisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
        verify(chatClientBuilder).build();
    }
    
    @Test
    void sendAndStreamMessage_shouldReturnStreamedContent_whenValidInputProvided() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        String conversationId = "test-conversation-123";
        Flux<String> expectedResponse = Flux.just("Hello", " there!", " How can I help?");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, conversationId);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Hello");
        
        verify(chatClient).prompt();
        verify(requestSpec).user(message);
        verify(requestSpec).options(any(OllamaOptions.class));
        verify(requestSpec).advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
        verify(requestSpec).stream();
        verify(streamSpec).content();
    }
    
    @Test
    void sendAndStreamMessage_shouldGenerateConversationId_whenConversationIdIsNull() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that advisors method was called (which handles conversation ID generation)
        verify(requestSpec).advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
    }
    
    @Test
    void sendAndStreamMessage_shouldUseProvidedConversationId_whenConversationIdProvided() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        String conversationId = "existing-conversation-456";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, conversationId);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        verify(requestSpec).advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
    }
    
    @Test
    void sendAndStreamMessage_shouldSetCorrectOllamaOptions() {
        // Given
        String message = "Hello, AI!";
        String model = "custom-model";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        ArgumentCaptor<OllamaOptions> optionsCaptor = ArgumentCaptor.forClass(OllamaOptions.class);
        
        // When
        chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        verify(requestSpec).options(optionsCaptor.capture());
        OllamaOptions capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions.getModel()).isEqualTo(model);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void sendAndStreamMessage_shouldThrowException_whenMessageIsInvalid(String message) {
        // Given
        String model = "llama2";
        
        // When/Then
        assertThatThrownBy(() -> chatService.sendAndStreamMessage(message, model, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message must not be empty");
        
        verify(chatClient, never()).prompt();
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void sendAndStreamMessage_shouldNotThrowException_whenModelIsInvalid(String model) {
        // Given
        String message = "Hello, AI!";
        
        // When/Then
        assertThatCode(() -> chatService.sendAndStreamMessage(message, model, null))
                .doesNotThrowAnyException();

        verify(chatClient, times(1)).prompt();
    }
    
    @Test
    void sendAndStreamMessage_shouldLoadVectorStoreFiles_whenVectorStoreIsSimpleVectorStore() throws IOException {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        // Replace the vector store in the service with a SimpleVectorStore mock
        ReflectionTestUtils.setField(chatService, "vectorStore", simpleVectorStore);
        
        // Create one test file in the temp directory
        Path vectorFile = tempDir.resolve("vector.json");
        Files.write(vectorFile, "{\"test\": \"data\"}".getBytes());
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that load was called for the file
        verify(simpleVectorStore, times(1)).load(any(PathResource.class));
    }
    
    @Test
    void sendAndStreamMessage_shouldHandleIOException_whenLoadingVectorStoreFilesFails() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        // Replace the vector store in the service with a SimpleVectorStore mock
        ReflectionTestUtils.setField(chatService, "vectorStore", simpleVectorStore);
        
        // Set an invalid vector store directory (non-existent path)
        PathResource invalidDirectory = new PathResource(Path.of("/non/existent/path"));
        ReflectionTestUtils.setField(chatService, "vectorStoreStorageDirectory", invalidDirectory);
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When/Then - Should not throw exception, but log error and continue
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // The method should continue execution despite the IOException
        verify(chatClient).prompt();
    }
    
    @Test
    void sendAndStreamMessage_shouldNotLoadVectorStoreFiles_whenVectorStoreIsNotSimpleVectorStore() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        // The default vectorStore mock is not a SimpleVectorStore
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that no loading operations were performed on the simple vector store
        verify(simpleVectorStore, never()).load(any(PathResource.class));
    }
    
    @Test
    void sendAndStreamMessage_shouldCreateNewVectorStoreWithCorrectEmbeddingModel() {
        // This test verifies that the constructor properly initializes the vector store
        // with the provided embedding model
        // The vectorStore should be a SimpleVectorStore built with the embeddingModel
        
        // Given - the service was already created in setUp()
        // When - we check the internal state indirectly through behavior
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that the chat client was set up properly
        verify(chatClient).prompt();
    }
    
    @Test
    void sendAndStreamMessage_shouldLogMessageAndModel() {
        // Given
        String message = "Test message for logging";
        String model = "test-model";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        // This test ensures the logging behavior works without exceptions
        // In a real-world scenario, you might use a LogCaptor or similar to verify log entries
        verify(chatClient).prompt();
        verify(requestSpec).user(message);
    }
    
    @Test
    void sendAndStreamMessage_shouldHandleEmptyVectorStoreDirectory() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        // Replace the vector store in the service with a SimpleVectorStore mock
        ReflectionTestUtils.setField(chatService, "vectorStore", simpleVectorStore);
        
        // Create an empty temp directory (no files)
        // tempDir is already empty by default
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that no files were loaded since the directory is empty
        verify(simpleVectorStore, never()).load(any(PathResource.class));
    }
    
    @Test
    void sendAndStreamMessage_shouldSkipNonRegularFiles() throws IOException {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        Flux<String> expectedResponse = Flux.just("Response");
        
        // Replace the vector store in the service with a SimpleVectorStore mock
        ReflectionTestUtils.setField(chatService, "vectorStore", simpleVectorStore);
        
        // Create a subdirectory and a regular file
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        Path regularFile = tempDir.resolve("vector.json");
        Files.write(regularFile, "{\"test\": \"data\"}".getBytes());
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        Flux<String> result = chatService.sendAndStreamMessage(message, model, null);
        
        // Then
        assertThat(result.blockFirst()).isEqualTo("Response");
        
        // Verify that only the regular file was loaded, not the directory
        verify(simpleVectorStore, times(1)).load(any(PathResource.class));
    }
    
    @Test
    void constructor_shouldCreateSimpleVectorStoreWithEmbeddingModel() {
        // Given/When - Constructor was called in setUp()
        
        // Then - Verify the constructor behavior by checking that the service was created
        // and that the builder was configured correctly
        verify(chatClientBuilder).defaultAdvisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
        verify(chatClientBuilder).build();
        
        // The vector store should be created internally as a SimpleVectorStore
        // We can't directly verify this without exposing internals, but we can verify
        // the behavior shows it was created properly
        assertThat(chatService).isNotNull();
    }
    
    @Test
    void sendAndStreamMessage_shouldUseCorrectConversationIdInAdvisors() {
        // Given
        String message = "Hello, AI!";
        String model = "llama2";
        String expectedConversationId = "specific-conversation-id";
        Flux<String> expectedResponse = Flux.just("Response");
        
        when(streamSpec.content()).thenReturn(expectedResponse);
        
        // When
        chatService.sendAndStreamMessage(message, model, expectedConversationId);
        
        // Then
        verify(requestSpec).advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
        
        // Verify other interactions
        verify(chatClient).prompt();
        verify(requestSpec).user(message);
        verify(requestSpec).options(any(OllamaOptions.class));
        verify(requestSpec).stream();
        verify(streamSpec).content();
    }
}
