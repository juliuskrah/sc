package org.simplecommerce.ai.commerce.config;

import java.net.URI;
import java.util.Map;

/**
 * Represents the configuration properties for the CLI. Most properties can be set via the CLI.
 * 
 * Some properties can be set only via environment variables e.g. API keys.
 * 
 * @author Julius Krah
 */
public record Config(
    Provider provider,
    Map<Provider, ProviderSetting> providers
) {
    public enum Provider {
        OLLAMA("ollama"),
        OPENAI("openai");

        private final String value;

        Provider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
    
    public record ProviderSetting (
        URI baseUrl,
        String model,
        Map<String, String> options
    ) {}

}
