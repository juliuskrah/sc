package org.simplecommerce.ai.commerce.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest(classes = ConfigService.class)
class ConfigServiceTest {

    @Test
    void getDir_returnsAbsolutePathIfDirectoryExists(@Autowired ConfigService service) {
        String result = service.getDir();
        assertThat(result).isEqualTo("%s/.sc", System.getProperty("user.dir"));
    }

    @Test
    void getFilePath_returnsAbsolutePathIfFileExists(@Autowired ConfigService service) {
        String result = service.getFilePath();
        assertThat(result).isEqualTo("%s/.sc/config", System.getProperty("user.dir"));
    }

    @Nested
    @TestPropertySource(properties = "sc.config.dir=/path/to/nonexistent/dir")
    class InvalidDirectoryConfig {

        @Test
        void getDir_returnsNullIfDirectoryDoesNotExist(@Autowired ConfigService service) {
            String result = service.getDir();
            assertThat(result).isNull();
        }

        @Test
        void getFilePath_returnsNullIfFileDoesNotExist(@Autowired ConfigService service) {
            String result = service.getFilePath();
            assertThat(result).isNull();
        }
    }

}
