package org.sc.ai.cli.chat.multimodal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A completer that provides file path completion when the '@' symbol is typed.
 * Only suggests image files (jpg, jpeg, png, gif, webp, bmp).
 * 
 * @author Julius Krah
 */
@Component
public class FilePathCompleter implements Completer {
    
    private static final Logger logger = LoggerFactory.getLogger(FilePathCompleter.class);
    
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();
        int cursor = line.cursor();
        
        // Find the last '@' symbol before the cursor
        int atIndex = findLastAtSymbol(buffer, cursor);
        if (atIndex == -1) {
            return; // No '@' symbol found
        }
        
        // Extract the partial path after '@'
        String partialPath = buffer.substring(atIndex + 1, cursor);
        
        // Handle quoted paths
        boolean isQuoted = false;
        char quoteChar = 0;
        if (partialPath.startsWith("\"") || partialPath.startsWith("'")) {
            isQuoted = true;
            quoteChar = partialPath.charAt(0);
            partialPath = partialPath.substring(1);
        }
        
        try {
            addPathCandidates(partialPath, candidates, isQuoted, quoteChar);
        } catch (Exception e) {
            logger.debug("Error during file completion", e);
        }
    }
    
    /**
     * Finds the last '@' symbol before the cursor position.
     * 
     * @param buffer the input buffer
     * @param cursor the cursor position
     * @return the index of the last '@' symbol, or -1 if not found
     */
    private int findLastAtSymbol(String buffer, int cursor) {
        for (int i = cursor - 1; i >= 0; i--) {
            char c = buffer.charAt(i);
            if (c == '@') {
                return i;
            }
            if (Character.isWhitespace(c) && i < cursor - 1) {
                break; // Don't cross word boundaries
            }
        }
        return -1;
    }
    
    /**
     * Adds file path candidates based on the partial path.
     * 
     * @param partialPath the partial path to complete
     * @param candidates the list to add candidates to
     * @param isQuoted whether the path is quoted
     * @param quoteChar the quote character used
     * @throws IOException if there's an error reading the file system
     */
    private void addPathCandidates(String partialPath, List<Candidate> candidates, 
                                  boolean isQuoted, char quoteChar) throws IOException {
        Path basePath = determineBasePath(partialPath);
        String prefix = determinePrefix(partialPath);
        
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return;
        }
        
        try (Stream<Path> files = Files.list(basePath)) {
            files.filter(path -> shouldIncludePath(path, prefix))
                 .forEach(path -> addCandidate(candidates, path, basePath, isQuoted, quoteChar));
        }
    }
    
    /**
     * Determines the base directory path for completion.
     * 
     * @param partialPath the partial path
     * @return the base directory path
     */
    private Path determineBasePath(String partialPath) {
        if (partialPath.isEmpty()) {
            return Paths.get(System.getProperty("user.dir"));
        }
        
        Path path = Paths.get(partialPath);
        if (path.isAbsolute()) {
            return path.getParent() != null ? path.getParent() : path;
        } else {
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            Path resolved = currentDir.resolve(path);
            return resolved.getParent() != null ? resolved.getParent() : currentDir;
        }
    }
    
    /**
     * Determines the filename prefix for filtering.
     * 
     * @param partialPath the partial path
     * @return the filename prefix
     */
    private String determinePrefix(String partialPath) {
        if (partialPath.isEmpty()) {
            return "";
        }
        
        Path path = Paths.get(partialPath);
        return path.getFileName() != null ? path.getFileName().toString() : "";
    }
    
    /**
     * Checks if a path should be included in completion results.
     * 
     * @param path the path to check
     * @param prefix the filename prefix to match
     * @return true if the path should be included
     */
    private boolean shouldIncludePath(Path path, String prefix) {
        String fileName = path.getFileName().toString();
        
        if (!fileName.toLowerCase().startsWith(prefix.toLowerCase())) {
            return false;
        }
        
        // Include directories for navigation
        if (Files.isDirectory(path)) {
            return true;
        }
        
        // Include only image files
        return isImageFile(fileName);
    }
    
    /**
     * Checks if a filename represents an image file.
     * 
     * @param fileName the filename to check
     * @return true if it's an image file
     */
    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || 
               lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || 
               lower.endsWith(".gif") || 
               lower.endsWith(".webp") || 
               lower.endsWith(".bmp");
    }
    
    /**
     * Adds a candidate to the completion list.
     * 
     * @param candidates the list to add to
     * @param path the path to add
     * @param basePath the base directory path
     * @param isQuoted whether the path is quoted
     * @param quoteChar the quote character used
     */
    private void addCandidate(List<Candidate> candidates, Path path, Path basePath, 
                             boolean isQuoted, char quoteChar) {
        String relativePath = basePath.relativize(path).toString();
        
        // Add trailing slash for directories
        if (Files.isDirectory(path)) {
            relativePath += "/";
        }
        
        // Handle quoting
        String value = relativePath;
        if (isQuoted) {
            value = quoteChar + relativePath;
            if (!Files.isDirectory(path)) {
                value += quoteChar; // Close quote for files
            }
        } else if (relativePath.contains(" ")) {
            value = "\"" + relativePath + "\""; // Auto-quote paths with spaces
        }
        
        String description = Files.isDirectory(path) ? "directory" : "image file";
        
        candidates.add(new Candidate(
            value,
            relativePath,
            null,
            description,
            null,
            null,
            true
        ));
    }
}
