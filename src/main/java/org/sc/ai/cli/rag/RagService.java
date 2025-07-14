package org.sc.ai.cli.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.slf4j.Logger;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Service for handling RAG (Retrieval-Augmented Generation) operations.
 */
@Service
public class RagService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RagService.class);
    private final ResourceLoader resourceLoader;
    private final EmbeddingModel embeddingModel;
    private DocumentReader documentReader;
    private DocumentTransformer documentTransformer;
    private DocumentWriter documentWriter;
    @Value("${sc.vector.simple.store:}")
    private PathResource vectorStoreStorageDirectory;

    public RagService(ResourceLoader resourceLoader, EmbeddingModel embeddingModel) {
        this.resourceLoader = resourceLoader;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Process a document and save to file.
     * 
     * @param documentUri The URI of the document to process
     * @param outputPath The path to save the processed output, or null to use default
     * @return The path where the output was saved
     * @throws IOException if there's an error processing the document
     */
    public Path processToFile(String documentUri, Path outputPath) throws IOException {
        logger.info("Processing document {} to file {}", documentUri, outputPath);
        return processLocalFile(documentUri, outputPath);
    }

    /**
     * Process a document and save to vector store.
     * 
     * @param documentUri The URI of the document to process
     * @throws IOException if there's an error processing the document
     */
    public void processToVectorStore(String documentUri) throws IOException {
        logger.info("Processing document {} to vector store", documentUri);
        processToSimpleVectorstore(documentUri);
    }

    private Path processLocalFile(String location, Path outputFile) throws IOException {
        var resource = resourceLoader.getResource(location);
        documentReader = determineReader(resource);
        documentTransformer = new TokenTextSplitter(true);
        documentWriter = new FileDocumentWriter(outputFile.toString(), true, MetadataMode.ALL, false);
        etl();
        if (Files.notExists(outputFile)) {
            throw new IOException("File not found: " + outputFile);
        }
        return outputFile;
    }

    private void processToSimpleVectorstore(String location) throws IOException {
        var resource = resourceLoader.getResource(location);
        documentReader = determineReader(resource);
        documentTransformer = new TokenTextSplitter(true);
        var vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        var vectorDir = Path.of(vectorStoreStorageDirectory.getURI());
        if (Files.notExists(vectorDir)) {
            Files.createDirectories(vectorDir);
        }
        var fileName = Instant.now().toEpochMilli() + ".json";
        Path vectorStorePath = vectorDir.resolve(fileName);
        documentWriter = vectorStore;
        etl();
        vectorStore.save(vectorStorePath.toFile());
    }

    DocumentReader determineReader(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            logger.warn("Resource filename is null, defaulting to TextReader");
            return new TextReader(resource);
        }
        
        String extension = getFileExtension(filename.toLowerCase());
        
        return switch (extension) {
            case "json" -> {
                logger.debug("Using JsonReader for file: {}", filename);
                yield new JsonReader(resource);
            }
            case "pdf" -> {
                logger.debug("Using PagePdfDocumentReader for file: {}", filename);
                yield new PagePdfDocumentReader(resource);
            }
            case "md", "markdown" -> {
                logger.debug("Using MarkdownDocumentReader for file: {}", filename);
                yield new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig());
            }
            case "html", "htm" -> {
                logger.debug("Using JsoupDocumentReader for file: {}", filename);
                yield new JsoupDocumentReader(resource);
            }
            default -> {
                // Default to TextReader for .txt files and any other text-based files
                logger.debug("Using TextReader for file: {} (default for unrecognized extension)", filename);
                yield new TextReader(resource);
            }
        };
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }

    private void etl() {
        documentWriter.write(documentTransformer.transform(documentReader.read()));
    }
}
