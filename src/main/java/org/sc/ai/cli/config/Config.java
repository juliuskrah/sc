package org.sc.ai.cli.config;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Represents the configuration properties for the CLI. Most properties can be
 * set via the CLI.
 * 
 * Some properties can be set only via environment variables e.g. API keys.
 * 
 * @author Julius Krah
 */
public record Config(
        ProviderType provider,
        Map<ProviderType, ProviderSettings> providers,
        ChatMemorySettings chatMemory) {

    public ProviderSettings resolvedProviderConfig(ProviderType provider) {
        return providers.get(provider);
    }

    public ChatMemory resolvedChatMemory() {
        return chatMemory.resolve();
    }

    public enum ProviderType {
        OLLAMA("ollama"),
        OPENAI("openai");

        private final String value;

        ProviderType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public record JdbcMemorySettings(URI url, String username) implements ChatMemory {
    }

    public record CassandraMemorySettings(List<URI> contactPoints, String keyspace) implements ChatMemory {
    }

    public sealed interface ChatMemory permits JdbcMemorySettings, CassandraMemorySettings {
    }

    // ChatMemory wrapper
    public record ChatMemorySettings(JdbcMemorySettings jdbc, CassandraMemorySettings cassandra) {
        /**
         * Resolves the chat memory configuration.
         * 
         * @return the configured ChatMemory instance, or null if no memory is
         *         configured.
         * @throws IllegalStateException if both jdbc and cassandra are set.
         */
        public ChatMemory resolve() {
            if (jdbc != null && cassandra != null) {
                throw new IllegalStateException("Only one of 'jdbc' or 'cassandra' must be set.");
            }
            if (jdbc != null)
                return jdbc;
            if (cassandra != null)
                return cassandra;
            return null; // No chat memory configured
        }
    }

    public record ProviderSettings(
            URI baseUrl,
            String model,
            Map<String, String> options) {
    }

}
