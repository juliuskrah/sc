package org.sc.ai.cli.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sc.ai.cli.config.Config.ProviderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
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

    @Nested
    @TestPropertySource(properties = "sc.config.dir=${java.io.tmpdir}/.config-test")
    class ConfigInitAndOpsTest {
        @Autowired
        private ConfigService service;
        @Value("${sc.config.dir}")
        private PathResource configDirResource;

        private Path configDir;

        @BeforeEach
        void setUp() throws IOException {
            configDir = Path.of(configDirResource.getURI());
            service.init();
        }

        @AfterEach
        void tearDown() throws IOException {
            if (Files.exists(configDir)) {
                // Recursively delete
                try (var stream = Files.walk(configDir)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException _) {
                            // ignore
                        }
                    });
                }
            }
        }

        @Test
        void init_createsDirectoryAndFile() {
            assertThat(configDir).exists().isDirectory();
            assertThat(configDir.resolve("config")).exists().isRegularFile();
        }

        @Test
        void setAndGet_aValue() {
            service.set(Map.of("provider", ProviderType.OLLAMA.getValue()));
            String value = service.get("provider");
            assertThat(value).isEqualTo(ProviderType.OLLAMA.getValue());
            var yamlFile = configDir.resolve("config");
            assertThat(yamlFile).content().isEqualTo("""
                    provider: ollama
                    """);
        }

        @Test
        void get_aNonExistentValue_returnsNull() {
            String value = service.get("non.existent");
            assertThat(value).isNull();
        }

        @Test
        void list_allValues() {
            service.set(Map.of(
                    "providers.ollama.base-url", "http://localhost:11433",
                    "providers.ollama.model", "mistral-small3.1"));
            Map<String, String> values = service.list();
            assertThat(values).containsEntry("providers.ollama.base-url", "http://localhost:11433")
                    .containsEntry("providers.ollama.model", "mistral-small3.1");
        }

        @Test
        void unset_aValue() {
            service.set(Map.of(
                    "chat-memory.jdbc.url", "jdbc:h2:mem:testdb",
                    "chat-memory.jdbc.username", "sa",
                    "providers.ollama.base-url", "http://localhost:11435"));
            service.unset(List.of("chat-memory.jdbc.username"));
            Map<String, String> values = service.list();
            assertThat(values).containsEntry("chat-memory.jdbc.url", "jdbc:h2:mem:testdb")
                    .containsEntry("providers.ollama.base-url", "http://localhost:11435")
                    .doesNotContainKey("chat-memory.jdbc.username");
        }

        @Test
        void get_withDottedKeyNotation() throws IOException {
            // Create a YAML configuration file
            Path config = configDir.resolve("config");
            Files.writeString(config, """
                    provider: ollama
                    providers:
                        ollama:
                            base-url: http://localhost:11433
                            model: mistral-small3.1
                        openai:
                            base-url: https://api.openai.com/v1
                            model: gpt-3.5-turbo
                            options: { } # provider-specific options
                    chat-memory:
                        jdbc:
                            url: jdbc:hsqldb:mem:testdb
                            username: sa
                    """);
            String provider = service.get("provider");
            assertThat(provider).isEqualTo("ollama");

            String ollamaBaseUrl = service.get("providers.ollama.base-url");
            assertThat(ollamaBaseUrl).isEqualTo("http://localhost:11433");
            String ollamaModel = service.get("providers.ollama.model");
            assertThat(ollamaModel).isEqualTo("mistral-small3.1");
            String openaiBaseUrl = service.get("providers.openai.base-url");
            assertThat(openaiBaseUrl).isEqualTo("https://api.openai.com/v1");
            String openaiModel = service.get("providers.openai.model");
            assertThat(openaiModel).isEqualTo("gpt-3.5-turbo");
            String jdbcUrl = service.get("chat-memory.jdbc.url");
            assertThat(jdbcUrl).isEqualTo("jdbc:hsqldb:mem:testdb");
            String jdbcUsername = service.get("chat-memory.jdbc.username");
            assertThat(jdbcUsername).isEqualTo("sa");
            String nonExistent = service.get("non.existent");
            assertThat(nonExistent).isNull();
        }

        @Test
        void set_mutuallyExclusiveValues() throws IOException {
            // Create a YAML configuration file
            Path config = configDir.resolve("config");
            Files.writeString(config, """
                    provider: ollama
                    providers:
                        ollama:
                            base-url: http://localhost:11433
                            model: mistral-small3.1
                        openai:
                            base-url: https://api.openai.com/v1
                            model: gpt-3.5-turbo
                            options: { } # provider-specific options
                    chat-memory:
                        jdbc:
                            url: jdbc:hsqldb:mem:testdb
                            username: sa
                    """);
            assertThatIllegalStateException()
                    .isThrownBy(() -> service.set(Map.of("chat-memory.cassandra.keyspace", "springboot")))
                    .withMessageContaining("Only one of 'jdbc' or 'cassandra' must be set.");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.set(Map.of("foo", "bar")))
                    .withMessageContaining("Unknown key: foo");

            // Confirm the config contents remain unchanged (reformatted but not altered)
            assertThat(config).content().isEqualTo("""
                    provider: ollama
                    chat-memory:
                      jdbc:
                        url: jdbc:hsqldb:mem:testdb
                        username: sa
                    providers:
                      openai:
                        base-url: https://api.openai.com/v1
                        model: gpt-3.5-turbo
                      ollama:
                        base-url: http://localhost:11433
                        model: mistral-small3.1
                    """);
        }

        @Test
        void flattenMap_convertsNestedStructureToFlatKeys() {
            Map<String, Object> nestedMap = Map.of(
                    "provider", "ollama",
                    "providers", Map.of(
                            "ollama", Map.of(
                                    "base-url", "http://localhost:11433",
                                    "model", "mistral-small3.1"),
                            "openai", Map.of(
                                    "base-url", "https://api.openai.com/v1",
                                    "model", "gpt-3.5-turbo",
                                    "options", Map.of())),
                    "chat-memory", Map.of(
                            "jdbc", Map.of(
                                    "url", "jdbc:hsqldb:mem:testdb",
                                    "username", "sa")));

            Map<String, String> flattened = service.flattenMap(nestedMap);

            assertThat(flattened)
                    .containsEntry("provider", "ollama")
                    .containsEntry("providers.ollama.base-url", "http://localhost:11433")
                    .containsEntry("providers.ollama.model", "mistral-small3.1")
                    .containsEntry("providers.openai.base-url", "https://api.openai.com/v1")
                    .containsEntry("providers.openai.model", "gpt-3.5-turbo")
                    .containsEntry("chat-memory.jdbc.url", "jdbc:hsqldb:mem:testdb")
                    .containsEntry("chat-memory.jdbc.username", "sa");
        }

        @Test
        void unflattenMap_convertsFlatKeysToNestedStructure() {
            Map<String, String> flattenedMap = Map.of(
                    "provider", "ollama",
                    "providers.ollama.base-url", "http://localhost:11433",
                    "providers.ollama.model", "mistral-small3.1",
                    "chat-memory.jdbc.url", "jdbc:hsqldb:mem:testdb",
                    "chat-memory.jdbc.username", "sa");

            Map<String, Object> nested = service.unflattenMap(flattenedMap);

            assertThat(nested).containsEntry("provider", "ollama");

            @SuppressWarnings("unchecked")
            Map<String, Object> providers = (Map<String, Object>) nested.get("providers");
            assertThat(providers).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> ollama = (Map<String, Object>) providers.get("ollama");
            assertThat(ollama).isNotNull()
                    .containsEntry("base-url", "http://localhost:11433")
                    .containsEntry("model", "mistral-small3.1");

            @SuppressWarnings("unchecked")
            Map<String, Object> chatMemory = (Map<String, Object>) nested.get("chat-memory");
            assertThat(chatMemory).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> jdbc = (Map<String, Object>) chatMemory.get("jdbc");
            assertThat(jdbc).isNotNull()
                    .containsEntry("url", "jdbc:hsqldb:mem:testdb")
                    .containsEntry("username", "sa");
        }

        @Test
        void setAndUnset_withDottedKeyNotation() throws IOException {
            // Set initial values
            service.set(Map.of(
                    "provider", "ollama",
                    "providers.ollama.base-url", "http://localhost:11433",
                    "chat-memory.jdbc.url", "jdbc:hsqldb:mem:testdb"));

            // Verify the YAML structure is correctly created
            Path configFile = configDir.resolve("config");
            String content = Files.readString(configFile);

            // Assert that nested structure is correctly created
            assertThat(content).contains("provider: ollama")
                    .contains("providers:")
                    .contains("  ollama:")
                    .contains("    base-url: http://localhost:11433")
                    .contains("chat-memory:")
                    .contains("  jdbc:")
                    .contains("    url: jdbc:hsqldb:mem:testdb");

            // Add more values
            service.set(Map.of(
                    "providers.ollama.model", "mistral-small3.1",
                    "chat-memory.jdbc.username", "sa"));

            // Verify values are added to existing structure
            content = Files.readString(configFile);
            assertThat(content).contains("    model: mistral-small3.1")
                    .contains("    username: sa");

            // Unset a value
            service.unset(List.of("chat-memory.jdbc.username"));

            // Verify the value is removed but structure remains
            content = Files.readString(configFile);
            assertThat(content).contains("chat-memory:")
                    .contains("  jdbc:")
                    .contains("    url: jdbc:hsqldb:mem:testdb")
                    .doesNotContain("    username: sa");
        }

        @Test
        void loadYamlAsBean_parsesProviderOptions() throws Exception {
            Path config = configDir.resolve("config");
            Files.writeString(config, """
                    provider: ollama
                    providers:
                      ollama:
                        base-url: http://localhost:11433
                        model: mistral-small3.1
                        options:
                          timeout: '30'
                          gpu: 'true'
                    """);

            var method = ConfigService.class.getDeclaredMethod("loadYamlAsBean");
            method.setAccessible(true);
            Config cfg = (Config) method.invoke(service);
            var settings = cfg.resolvedProviderConfig(ProviderType.OLLAMA);

            assertThat(settings.options())
                    .containsEntry("timeout", "30")
                    .containsEntry("gpu", "true");
        }
    }

}
