package org.sc.ai.cli.chat.multimodal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PromptParser}.
 * 
 * @author Julius Krah
 */
@ExtendWith(MockitoExtension.class)
class PromptParserTest {

    private final PromptParser parser = new PromptParser();

    @Test
    void parse_shouldReturnTextOnly_whenNoAtSymbol() {
        // When
        var result = parser.parse("Hello world");
        
        // Then
        assertThat(result.textContent()).isEqualTo("Hello world");
        assertThat(result.filePaths()).isEmpty();
    }
    
    @Test
    void parse_shouldReturnEmpty_whenNullInput() {
        // When
        var result = parser.parse(null);
        
        // Then
        assertThat(result.textContent()).isEmpty();
        assertThat(result.filePaths()).isEmpty();
    }
    
    @Test
    void parse_shouldReturnEmpty_whenEmptyInput() {
        // When
        var result = parser.parse("");
        
        // Then
        assertThat(result.textContent()).isEmpty();
        assertThat(result.filePaths()).isEmpty();
    }
    
    @Test
    void parse_shouldExtractFilePath_whenValidImageFile(@TempDir Path tempDir) throws IOException {
        // Given
        Path imageFile = tempDir.resolve("test.jpg");
        Files.createFile(imageFile);
        String prompt = "Describe this image @" + imageFile.toString();
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Describe this image");
        assertThat(result.filePaths()).hasSize(1);
        assertThat(result.filePaths().get(0)).isEqualTo(imageFile);
    }
    
    @Test
    void parse_shouldHandleQuotedPaths(@TempDir Path tempDir) throws IOException {
        // Given
        Path imageFile = tempDir.resolve("test image.png");
        Files.createFile(imageFile);
        String prompt = "Describe @\"" + imageFile.toString() + "\" please";
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Describe please");
        assertThat(result.filePaths()).hasSize(1);
        assertThat(result.filePaths().get(0)).isEqualTo(imageFile);
    }
    
    @Test
    void parse_shouldHandleSingleQuotedPaths(@TempDir Path tempDir) throws IOException {
        // Given
        Path imageFile = tempDir.resolve("test.webp");
        Files.createFile(imageFile);
        String prompt = "Look at @'" + imageFile.toString() + "'";
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Look at");
        assertThat(result.filePaths()).hasSize(1);
        assertThat(result.filePaths().get(0)).isEqualTo(imageFile);
    }
    
    @Test
    void parse_shouldIgnoreNonExistentFiles() {
        // Given
        String prompt = "Show me @/nonexistent/path/image.jpg please";
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Show me please");
        assertThat(result.filePaths()).isEmpty();
    }
    
    @Test
    void parse_shouldIgnoreNonImageFiles(@TempDir Path tempDir) throws IOException {
        // Given
        Path textFile = tempDir.resolve("document.txt");
        Files.createFile(textFile);
        String prompt = "Read @" + textFile.toString();
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Read");
        assertThat(result.filePaths()).isEmpty();
    }
    
    @Test
    void parse_shouldHandleMultipleImages(@TempDir Path tempDir) throws IOException {
        // Given
        Path image1 = tempDir.resolve("image1.jpg");
        Path image2 = tempDir.resolve("image2.png");
        Files.createFile(image1);
        Files.createFile(image2);
        String prompt = "Compare @" + image1.toString() + " and @" + image2.toString();
        
        // When
        var result = parser.parse(prompt);
        
        // Then
        assertThat(result.textContent()).isEqualTo("Compare and");
        assertThat(result.filePaths()).hasSize(2);
        assertThat(result.filePaths()).containsExactly(image1, image2);
    }
    
    @Test
    void parse_shouldHandleRelativePaths() throws IOException {
        // Given - create file in current working directory
        String currentDir = System.getProperty("user.dir");
        Path workingDir = Path.of(currentDir);
        Path imageFile = workingDir.resolve("test-image.gif");
        
        try {
            Files.createFile(imageFile);
            String prompt = "Show me @test-image.gif";
            
            // When
            var result = parser.parse(prompt);
            
            // Then
            assertThat(result.textContent()).isEqualTo("Show me");
            assertThat(result.filePaths()).hasSize(1);
            assertThat(result.filePaths().get(0)).isEqualTo(imageFile);
        } finally {
            // Clean up
            Files.deleteIfExists(imageFile);
        }
    }
    
    @Test
    void parse_shouldSupportAllImageExtensions(@TempDir Path tempDir) throws IOException {
        // Given
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};
        StringBuilder promptBuilder = new StringBuilder("Show all images: ");
        
        for (int i = 0; i < extensions.length; i++) {
            Path imageFile = tempDir.resolve("image" + i + extensions[i]);
            Files.createFile(imageFile);
            promptBuilder.append("@").append(imageFile.toString()).append(" ");
        }
        
        // When
        var result = parser.parse(promptBuilder.toString());
        
        // Then
        assertThat(result.textContent()).isEqualTo("Show all images:");
        assertThat(result.filePaths()).hasSize(extensions.length);
    }
}
