package org.sc.ai.cli.chat.multimodal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ParsedPrompt}.
 * 
 * @author Julius Krah
 */
class ParsedPromptTest {

    @Test
    void textOnly_shouldCreatePromptWithNoFiles() {
        // When
        var prompt = ParsedPrompt.textOnly("Hello world");
        
        // Then
        assertThat(prompt.textContent()).isEqualTo("Hello world");
        assertThat(prompt.filePaths()).isEmpty();
        assertThat(prompt.hasFiles()).isFalse();
        assertThat(prompt.fileCount()).isZero();
    }
    
    @Test
    void constructor_shouldCreatePromptWithFiles() {
        // Given
        List<Path> files = List.of(
            Paths.get("/path/to/image1.jpg"),
            Paths.get("/path/to/image2.png")
        );
        
        // When
        var prompt = new ParsedPrompt("Describe these images", files);
        
        // Then
        assertThat(prompt.textContent()).isEqualTo("Describe these images");
        assertThat(prompt.filePaths()).hasSize(2);
        assertThat(prompt.hasFiles()).isTrue();
        assertThat(prompt.fileCount()).isEqualTo(2);
    }
    
    @Test
    void hasFiles_shouldReturnFalseForEmptyList() {
        // When
        var prompt = new ParsedPrompt("Text only", List.of());
        
        // Then
        assertThat(prompt.hasFiles()).isFalse();
    }
    
    @Test
    void fileCount_shouldReturnCorrectCount() {
        // Given
        List<Path> files = List.of(
            Paths.get("/image1.jpg"),
            Paths.get("/image2.png"),
            Paths.get("/image3.gif")
        );
        
        // When
        var prompt = new ParsedPrompt("Three images", files);
        
        // Then
        assertThat(prompt.fileCount()).isEqualTo(3);
    }
}
