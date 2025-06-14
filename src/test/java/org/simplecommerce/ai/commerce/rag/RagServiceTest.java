package org.simplecommerce.ai.commerce.rag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

@SpringBootTest(classes = RagService.class)
class RagServiceTest {
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
        // Create a test text file
        Path inputFile = tempDir.resolve("test.txt");
        Files.writeString(inputFile, """
                Product Information:
                
                Brand: Specialized
                Model: Stumpjumper
                Type: Mountain Bike
                Description: All-mountain bike with excellent suspension.
                """);
        Path outputPath = tempDir.resolve("output.txt");

        ragService.processToFile("file://" + inputFile.toString(), outputPath);

        assertThat(outputPath).content()
                .contains("Specialized")
                .contains("Stumpjumper");
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
}
