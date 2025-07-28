package org.sc.ai.cli.chat.multimodal;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a parsed user prompt with potential file attachments.
 * 
 * @author Julius Krah
 */
public record ParsedPrompt(String textContent, List<Path> filePaths) {
    
    /**
     * Creates a text-only prompt with no file attachments.
     * 
     * @param textContent the text content
     * @return a ParsedPrompt with only text content
     */
    public static ParsedPrompt textOnly(String textContent) {
        return new ParsedPrompt(textContent, List.of());
    }
    
    /**
     * Checks if this prompt has any file attachments.
     * 
     * @return true if there are file attachments, false otherwise
     */
    public boolean hasFiles() {
        return !filePaths.isEmpty();
    }
    
    /**
     * Gets the number of attached files.
     * 
     * @return the number of attached files
     */
    public int fileCount() {
        return filePaths.size();
    }
}
