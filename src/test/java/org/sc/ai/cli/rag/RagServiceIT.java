package org.sc.ai.cli.rag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sc.ai.cli.AiTestConfiguration;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@TestPropertySource(properties = {"sc.vector.simple.store=/tmp/vector-store"})
@ContextConfiguration(classes = AiTestConfiguration.class)
@ActiveProfiles("test")
class RagServiceIT {
    @Autowired
    private RagService ragService;

    @Test
    void processToFile_shouldHandleLocalFile(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path inputFile = tempDir.resolve("test.json");
        Files.writeString(inputFile, """
                [
                    {
                    "id": 1,
                    "brand": "Trek",
                    "description": "A high-performance mountain bike for trail riding."
                    },
                    {
                    "id": 2,
                    "brand": "Cannondale",
                    "description": "An aerodynamic road bike for racing enthusiasts."
                    }
                ]
                """);
        Path outputPath = tempDir.resolve("output.txt");

        ragService.processToFile("file://" + inputFile.toString(), outputPath);

        assertThat(outputPath).content()
                .contains("{id=1, brand=Trek, description=A high-performance mountain bike for trail riding.}");
    }

    @Test
    void processToFile_shouldFailForNonexistentFile(@TempDir Path tempDir) {
        String nonExistentFile = Path.of("/non/existent/file.txt").toString();
        Path outputPath = tempDir.resolve("output.txt");

        assertThatThrownBy(() -> ragService.processToFile(nonExistentFile, outputPath))
                .hasCauseInstanceOf(FileNotFoundException.class)
                .hasMessageContaining(
                        "class path resource [non/existent/file.txt] cannot be opened because it does not exist");
    }

    @Test
    void processToFile_shouldHandleHttpsUrl(@TempDir Path tempDir) throws IOException {
        String httpsUri = "https://raw.githubusercontent.com/juliuskrah/simple-commerce/1eeb2c7f03105d1c2022784a0456c68bb8a39edc/app/src/main/resources/seed-data/categories.json";
        Path outputPath = tempDir.resolve("output.txt");

        ragService.processToFile(httpsUri, outputPath);

        assertThat(outputPath).content().contains("{id=7822a5bc-e768-4ccf-bcef-820d0a7148c5, title=Graphics and Digital Art, description=Stock photos, illustrations, icons, logos, design templates, wallpapers, and digital paintings.}");
    }

    // Test cases for determineReader method with different file types
    
    @Test
    void determineReader_shouldReturnJsonReaderForJsonFile(@TempDir Path tempDir) throws IOException {
        // Create a test JSON file
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
                {
                    "name": "test",
                    "value": "example"
                }
                """);
        
        FileSystemResource resource = new FileSystemResource(jsonFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(JsonReader.class);
    }

    @Test
    void determineReader_shouldReturnTextReaderForTxtFile(@TempDir Path tempDir) throws IOException {
        // Create a test text file
        Path txtFile = tempDir.resolve("test.txt");
        Files.writeString(txtFile, "This is a simple text file content.");
        
        FileSystemResource resource = new FileSystemResource(txtFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(TextReader.class);
    }

    @Test
    void determineReader_shouldReturnMarkdownReaderForMdFile(@TempDir Path tempDir) throws IOException {
        // Create a test markdown file
        Path mdFile = tempDir.resolve("test.md");
        Files.writeString(mdFile, """
                # Test Markdown
                
                This is a **test** markdown file with some content.
                
                ## Section
                - Item 1
                - Item 2
                """);
        
        FileSystemResource resource = new FileSystemResource(mdFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(MarkdownDocumentReader.class);
    }

    @Test
    void determineReader_shouldReturnMarkdownReaderForMarkdownFile(@TempDir Path tempDir) throws IOException {
        // Create a test markdown file with .markdown extension
        Path markdownFile = tempDir.resolve("test.markdown");
        Files.writeString(markdownFile, """
                # Test Markdown Document
                
                This is a **test** markdown file with `.markdown` extension.
                """);
        
        FileSystemResource resource = new FileSystemResource(markdownFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(MarkdownDocumentReader.class);
    }

    @Test
    void determineReader_shouldReturnJsoupReaderForHtmlFile(@TempDir Path tempDir) throws IOException {
        // Create a test HTML file
        Path htmlFile = tempDir.resolve("test.html");
        Files.writeString(htmlFile, """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test HTML</title>
                </head>
                <body>
                    <h1>Test Content</h1>
                    <p>This is a test HTML document.</p>
                </body>
                </html>
                """);
        
        FileSystemResource resource = new FileSystemResource(htmlFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(JsoupDocumentReader.class);
    }

    @Test
    void determineReader_shouldReturnJsoupReaderForHtmFile(@TempDir Path tempDir) throws IOException {
        // Create a test HTM file
        Path htmFile = tempDir.resolve("test.htm");
        Files.writeString(htmFile, """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test HTM</title>
                </head>
                <body>
                    <h1>Test Content</h1>
                    <p>This is a test HTM document.</p>
                </body>
                </html>
                """);
        
        FileSystemResource resource = new FileSystemResource(htmFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(JsoupDocumentReader.class);
    }

    @Test
    void determineReader_shouldReturnPdfReaderForPdfFile() {
        var pdfResource = new ClassPathResource("raft.pdf");
        DocumentReader reader = ragService.determineReader(pdfResource);
        assertThat(reader).isInstanceOf(PagePdfDocumentReader.class);
    }

    @Test
    void determineReader_shouldReturnTextReaderForUnknownExtension(@TempDir Path tempDir) throws IOException {
        // Create a test file with unknown extension
        Path unknownFile = tempDir.resolve("test.xyz");
        Files.writeString(unknownFile, "Content with unknown file extension");
        
        FileSystemResource resource = new FileSystemResource(unknownFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(TextReader.class);
    }

    @Test
    void determineReader_shouldReturnTextReaderForFileWithoutExtension(@TempDir Path tempDir) throws IOException {
        // Create a test file without extension
        Path noExtFile = tempDir.resolve("README");
        Files.writeString(noExtFile, "File without extension");
        
        FileSystemResource resource = new FileSystemResource(noExtFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(TextReader.class);
    }

    @Test
    void determineReader_shouldHandleCaseInsensitiveExtensions(@TempDir Path tempDir) throws IOException {
        // Test case insensitive extension matching
        Path upperCaseJsonFile = tempDir.resolve("test.JSON");
        Files.writeString(upperCaseJsonFile, """
                {
                    "test": "uppercase extension"
                }
                """);
        
        FileSystemResource resource = new FileSystemResource(upperCaseJsonFile);
        DocumentReader reader = ragService.determineReader(resource);
        
        assertThat(reader).isInstanceOf(JsonReader.class);
    }

    @Test
    void determineReader_shouldHandleNonExistentResource() {
        // Create a ClassPathResource for a non-existent file
        ClassPathResource nonExistentResource = new ClassPathResource("non-existent-file.txt");
        
        // The determineReader method should still return a TextReader for a non-existent file
        // This test verifies it doesn't throw an exception when the resource doesn't exist
        DocumentReader reader = ragService.determineReader(nonExistentResource);
        
        assertThat(reader).isInstanceOf(TextReader.class);
    }

    // Integration tests for processing different file types end-to-end
    
    @Test
    void processToFile_shouldHandleMarkdownFile(@TempDir Path tempDir) throws IOException {
        // Create a test markdown file
        Path inputFile = tempDir.resolve("test.md");
        Files.writeString(inputFile, """
                # Product Catalog
                
                ## Mountain Bikes
                **Trek X-Caliber** - High-performance trail bike for serious riders.
                
                ## Road Bikes  
                **Cannondale SuperSix** - Aerodynamic racing machine.
                """);
        Path outputPath = tempDir.resolve("output.txt");

        ragService.processToFile("file://" + inputFile.toString(), outputPath);

        assertThat(outputPath).content()
                .contains("Trek X-Caliber")
                .contains("Cannondale SuperSix");
    }

    @Test
    void processToFile_shouldHandleTextFile(@TempDir Path tempDir) throws IOException {
        Path outputPath = tempDir.resolve("output.txt");
        ragService.processToFile("classpath:mars.txt", outputPath);

        assertThat(outputPath).content()
                .contains("This paper provides a comprehensive examination of Mars")
                .contains("Mars has long fascinated humanity, both scientifically and culturally");
    }

    @Test
    void processToFile_shouldHandleHtmlFile(@TempDir Path tempDir) throws IOException {
        // Create a test HTML file
        Path inputFile = tempDir.resolve("test.html");
        Files.writeString(inputFile, """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Bike Catalog</title>
                </head>
                <body>
                    <h1>Featured Bikes</h1>
                    <div>
                        <h2>Giant Trance</h2>
                        <p>Full suspension mountain bike perfect for trails.</p>
                    </div>
                    <div>
                        <h2>Scott Addict</h2>
                        <p>Lightweight carbon road bike for competitive cycling.</p>
                    </div>
                </body>
                </html>
                """);
        Path outputPath = tempDir.resolve("output.txt");

        ragService.processToFile("file://" + inputFile.toString(), outputPath);

        assertThat(outputPath).content()
                .contains("Giant Trance")
                .contains("Scott Addict");
    }

    @Test
    void processToFile_shouldHandlePdfFile(@TempDir Path tempDir) throws IOException {
        Path outputPath = tempDir.resolve("output.txt");
        ragService.processToFile("classpath:raft.pdf", outputPath);

        assertThat(outputPath).content()
                .contains("In Search of an Understandable Consensus Algorithm")
                .contains("Replicated state machines are typically implemented ");
    }

    @Test
    void processToVectorStore_shouldHandleJsonFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary vector store directory
        Path vectorStoreDir = tempDir.resolve("vector-store");
        Files.createDirectories(vectorStoreDir);
        // Set the vector store directory to the temporary path
        ReflectionTestUtils.setField(ragService, "vectorStoreStorageDirectory", new PathResource(vectorStoreDir));
        
        // Create a test file
        Path inputFile = tempDir.resolve("test.json");
        Files.writeString(inputFile, """
                [
                    {
                    "id": 1,
                    "brand": "Trek",
                    "description": "A high-performance mountain bike for trail riding."
                    },
                    {
                    "id": 2,
                    "brand": "Cannondale",
                    "description": "An aerodynamic road bike for racing enthusiasts."
                    }
                ]
                """);

        // Process to vector store
        ragService.processToVectorStore("file://" + inputFile.toString());

        // Verify that vector store files were created
        long vectorStoreFiles = Files.list(vectorStoreDir)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        
        assertThat(vectorStoreFiles).isGreaterThan(0);
    }

    @Test
    void processToVectorStore_shouldHandleMarkdownFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary vector store directory
        Path vectorStoreDir = tempDir.resolve("vector-store");
        Files.createDirectories(vectorStoreDir);
        // Set the vector store directory to the temporary path
        ReflectionTestUtils.setField(ragService, "vectorStoreStorageDirectory", new PathResource(vectorStoreDir));
        
        // Create a test markdown file
        Path inputFile = tempDir.resolve("test.md");
        Files.writeString(inputFile, """
                # Product Catalog
                
                ## Mountain Bikes
                **Trek X-Caliber** - High-performance trail bike for serious riders.
                
                ## Road Bikes  
                **Cannondale SuperSix** - Aerodynamic racing machine.
                """);

        // Process to vector store
        ragService.processToVectorStore("file://" + inputFile.toString());

        // Verify that vector store files were created
        long vectorStoreFiles = Files.list(vectorStoreDir)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        
        assertThat(vectorStoreFiles).isGreaterThan(0);
    }

    @Test
    void processToVectorStore_shouldFailForNonexistentFile() {
        String nonExistentFile = Path.of("/non/existent/file.txt").toString();

        assertThatThrownBy(() -> ragService.processToVectorStore(nonExistentFile))
                .hasCauseInstanceOf(FileNotFoundException.class)
                .hasMessageContaining(
                        "class path resource [non/existent/file.txt] cannot be opened because it does not exist");
    }

    @Test
    void processToVectorStore_shouldHandleHttpsUrl(@TempDir Path tempDir) throws IOException {
        // Create a temporary vector store directory
        Path vectorStoreDir = tempDir.resolve("vector-store");
        Files.createDirectories(vectorStoreDir);
        // Set the vector store directory to the temporary path
        ReflectionTestUtils.setField(ragService, "vectorStoreStorageDirectory", new PathResource(vectorStoreDir));
        
        String httpsUri = "https://raw.githubusercontent.com/juliuskrah/simple-commerce/1eeb2c7f03105d1c2022784a0456c68bb8a39edc/app/src/main/resources/seed-data/categories.json";

        // Process to vector store
        ragService.processToVectorStore(httpsUri);

        // Verify that vector store files were created
        long vectorStoreFiles = Files.list(vectorStoreDir)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        
        assertThat(vectorStoreFiles).isGreaterThan(0);
    }

    @Test
    void processToVectorStore_shouldCreateDirectoryIfNotExists(@TempDir Path tempDir) throws IOException {
        // Create a path that doesn't exist yet
        Path vectorStoreDir = tempDir.resolve("non-existent-dir");
        // Set the vector store directory to the non-existent path
        ReflectionTestUtils.setField(ragService, "vectorStoreStorageDirectory", new PathResource(vectorStoreDir));
        
        // Create a test file
        Path inputFile = tempDir.resolve("test.txt");
        Files.writeString(inputFile, "This is a simple text file content.");

        // Process to vector store - this should create the directory
        ragService.processToVectorStore("file://" + inputFile.toString());

        // Verify that the directory was created
        assertThat(vectorStoreDir).exists().isDirectory();
        
        // Verify that vector store files were created
        long vectorStoreFiles = Files.list(vectorStoreDir)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        
        assertThat(vectorStoreFiles).isGreaterThan(0);
    }

    // Test with a real vector store directory but simpler test
    @Test
    void processToVectorStore_shouldHandlePdfFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary vector store directory
        Path vectorStoreDir = tempDir.resolve("vector-store");
        Files.createDirectories(vectorStoreDir);
        // Set the vector store directory to the temporary path
        ReflectionTestUtils.setField(ragService, "vectorStoreStorageDirectory", new PathResource(vectorStoreDir));
        
        // Process to vector store using a classpath resource
        ragService.processToVectorStore("classpath:raft.pdf");

        // Verify that vector store files were created
        long vectorStoreFiles = Files.list(vectorStoreDir)
                .filter(p -> p.toString().endsWith(".json"))
                .count();
        
        assertThat(vectorStoreFiles).isGreaterThan(0);
    }

    /**
     * Test for the etl() method through direct field manipulation and invocation
     */
    @Test
    void etl_shouldProcessDocumentsThroughTransformerToWriter(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path inputFile = tempDir.resolve("test.txt");
        Files.writeString(inputFile, "Sample content for ETL testing");
        Path outputPath = tempDir.resolve("etl-output.txt");
        
        // Prepare the objects directly but stop before calling etl()
        var resource = new FileSystemResource(inputFile);
        ReflectionTestUtils.setField(ragService, "documentReader", new TextReader(resource));
        ReflectionTestUtils.setField(ragService, "documentTransformer", new TokenTextSplitter(true));
        ReflectionTestUtils.setField(ragService, "documentWriter", new FileDocumentWriter(outputPath.toString(), true, org.springframework.ai.document.MetadataMode.ALL, false));
        
        // Invoke the etl method using reflection
        ReflectionTestUtils.invokeMethod(ragService, "etl");
        
        // Verify that the output file was created with expected content
        assertThat(outputPath)
            .exists()
            .content().contains("Sample content for ETL testing");
    }

    /**
     * Test for getFileExtension method using reflection
     */
    @Test
    void getFileExtension_shouldReturnCorrectExtension() {
        // Test with various filenames
        String pdf = (String) ReflectionTestUtils.invokeMethod(ragService, "getFileExtension", "document.pdf");
        assertThat(pdf).isEqualTo("pdf");
        
        String txt = (String) ReflectionTestUtils.invokeMethod(ragService, "getFileExtension", "file.with.multiple.dots.txt");
        assertThat(txt).isEqualTo("txt");
        
        String noExt = (String) ReflectionTestUtils.invokeMethod(ragService, "getFileExtension", "README");
        assertThat(noExt).isEmpty();
        
        String hidden = (String) ReflectionTestUtils.invokeMethod(ragService, "getFileExtension", ".htaccess");
        assertThat(hidden).isEqualTo("htaccess");
        
        String endingDot = (String) ReflectionTestUtils.invokeMethod(ragService, "getFileExtension", "file.");
        assertThat(endingDot).isEmpty();
    }
}
