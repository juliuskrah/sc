package org.sc.ai.cli.chat.multimodal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses user prompts to extract text content and file paths.
 * File paths are indicated by the @ symbol followed by the path.
 * 
 * @author Julius Krah
 */
public class PromptParser {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptParser.class);
    
    // Pattern to match @/path/to/file or @path/to/file (supports quotes for paths with spaces)
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
        "@(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+))", 
        Pattern.MULTILINE
    );
    
    /**
     * Parses a user prompt to extract text content and file paths.
     * 
     * @param prompt the user prompt
     * @return a ParsedPrompt containing text content and file paths
     */
    public ParsedPrompt parse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return ParsedPrompt.textOnly("");
        }
        
        List<Path> filePaths = new ArrayList<>();
        String textContent = prompt;
        
        Matcher matcher = FILE_PATH_PATTERN.matcher(prompt);
        
        while (matcher.find()) {
            String filePath = extractFilePathFromMatch(matcher);
            
            if (filePath != null) {
                textContent = processFilePath(filePath, filePaths, textContent, matcher.group(0));
            }
        }
        
        // Clean up the text content (remove extra spaces)
        textContent = textContent.replaceAll("\\s+", " ").trim();
        
        return new ParsedPrompt(textContent, filePaths);
    }
    
    /**
     * Extracts the file path from a matcher group.
     * 
     * @param matcher the regex matcher
     * @return the extracted file path, or null if none found
     */
    private String extractFilePathFromMatch(Matcher matcher) {
        // Check which group matched (quoted or unquoted)
        if (matcher.group(1) != null) {
            return matcher.group(1); // Double quoted
        } else if (matcher.group(2) != null) {
            return matcher.group(2); // Single quoted
        } else if (matcher.group(3) != null) {
            return matcher.group(3); // Unquoted
        }
        return null;
    }
    
    /**
     * Processes a file path, adding it to the list if valid and removing it from text content.
     * 
     * @param filePath the file path to process
     * @param filePaths the list to add valid paths to
     * @param textContent the current text content
     * @param matchedText the text that was matched and should be removed
     * @return the updated text content
     */
    private String processFilePath(String filePath, List<Path> filePaths, String textContent, String matchedText) {
        try {
            Path resolvedPath = resolvePath(filePath);
            if (isValidImageFile(resolvedPath)) {
                filePaths.add(resolvedPath);
                logger.debug("Found image file: {}", resolvedPath);
            } else {
                logger.warn("File does not exist or is not a supported image: {}", resolvedPath);
            }
        } catch (Exception e) {
            logger.warn("Invalid file path: {}", filePath, e);
        }
        
        // Remove the file path from text content
        return textContent.replace(matchedText, "");
    }
    
    /**
     * Resolves a file path, handling both relative and absolute paths.
     * 
     * @param filePath the file path to resolve
     * @return the resolved Path
     */
    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        
        if (path.isAbsolute()) {
            return path;
        } else {
            // Resolve relative to current working directory
            return Paths.get(System.getProperty("user.dir")).resolve(path);
        }
    }
    
    /**
     * Checks if a path points to a valid image file.
     * 
     * @param path the path to check
     * @return true if the path is a valid image file, false otherwise
     */
    private boolean isValidImageFile(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }
        
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jpg") || 
               fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || 
               fileName.endsWith(".gif") || 
               fileName.endsWith(".webp") || 
               fileName.endsWith(".bmp");
    }
}
