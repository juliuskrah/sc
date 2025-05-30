package org.simplecommerce.ai.commerce.rag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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


}
