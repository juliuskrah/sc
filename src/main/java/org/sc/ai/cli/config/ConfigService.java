package org.sc.ai.cli.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.HashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;

/**
 * @author Julius Krah
 */
@Service
public class ConfigService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigService.class);
    @Value("${sc.config.dir:}")
    private PathResource configDirectory;
    private static final String CONFIG_FILE_NAME = "config";

    private Config loadYamlAsBean() {
        Yaml yaml = getYaml(construct());
        yaml.setBeanAccess(org.yaml.snakeyaml.introspector.BeanAccess.FIELD);
        String filePath = getFilePath();
        if (filePath != null) {
            try (InputStream in = Files.newInputStream(Path.of(filePath))) {
                return yaml.load(in);
            } catch (IOException e) {
                logger.warn("Could not load config file", e);
            }
        }
        return null;
    }

    private Config.ProviderType getProviderType(String provider) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> Config.ProviderType.OLLAMA;
            case "openai" -> Config.ProviderType.OPENAI;
            case "bedrock" -> Config.ProviderType.BEDROCK;
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    private Map<Config.ProviderType, Config.ProviderSettings> getProviderSettings(
            Map<String, Object> map) {
        Map<Config.ProviderType, Config.ProviderSettings> providerSettingsMap = new java.util.EnumMap<>(Config.ProviderType.class);
        for (var entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> settings = (Map<String, Object>) value;
                Config.ProviderType providerType = getProviderType(key);
                URI baseUrl = null;
                String model = null;
                Map<String, String> options = new java.util.HashMap<>();
                for (var element : settings.entrySet()) {
                    if ("base-url".equals(element.getKey())) {
                        baseUrl = URI.create(element.getValue().toString());
                    } else if ("model".equals(element.getKey())) {
                        model = element.getValue().toString();
                    } else if ("options".equals(element.getKey()) && element.getValue() instanceof Map<?, ?> optMap) {
                        for (var optEntry : optMap.entrySet()) {
                            var optKey = optEntry.getKey();
                            var optVal = optEntry.getValue();
                            if (optKey != null && optVal != null) {
                                options.put(optKey.toString(), optVal.toString());
                            }
                        }
                    }
                }
                providerSettingsMap.put(providerType, new Config.ProviderSettings(baseUrl, model, options));
            }
        }
        return providerSettingsMap;
    }

    private Config.ChatMemorySettings getChatMemorySettings(Map<String, Object> map) {
        Config.JdbcMemorySettings jdbcMemorySettings = null;
        Config.CassandraMemorySettings cassandraMemorySettings = null;

        if (map.containsKey("jdbc")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> jdbcMap = (Map<String, Object>) map.get("jdbc");
            var url = jdbcMap.containsKey("url") ? URI.create(jdbcMap.get("url").toString()) : null;
            var username = jdbcMap.containsKey("username") ? jdbcMap.get("username").toString() : "";
            jdbcMemorySettings = new Config.JdbcMemorySettings(url, username);
        }
        if (map.containsKey("cassandra")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cassandraMap = (Map<String, Object>) map.get("cassandra");
            var keyspace = cassandraMap.containsKey("keyspace") ? cassandraMap.get("keyspace").toString() : "";
            var contactPointsObj = cassandraMap.get("contact-points");
            List<URI> contactPoints = List.of();
            if (contactPointsObj instanceof List<?> cp) {
                contactPoints = cp.stream().filter(Objects::nonNull)
                    .map(contactPoint -> URI.create(contactPoint.toString())).toList();
            }
            cassandraMemorySettings = new Config.CassandraMemorySettings(contactPoints, keyspace);
        }
        return new Config.ChatMemorySettings(jdbcMemorySettings, cassandraMemorySettings);
    }

    private Yaml getYaml(Constructor constructor) {
        return new Yaml(constructor);
    }

    private org.yaml.snakeyaml.constructor.Constructor construct() {
        @SuppressWarnings("unchecked")
        Map<Class<?>, BiFunction<Function<Node, Object>, MappingNode, Object>> mappingNodeConstructors = Map.of(Config.class, (construct, node) -> {
                    Config.ProviderType providerType = null;
                    Map<Config.ProviderType, Config.ProviderSettings> providerSettings = Map.of();
                    Config.ChatMemorySettings chatMemorySettings = null;
            
                    for (var tuple : node.getValue()) {
                        var keyNode = tuple.getKeyNode();
                        var valueNode = tuple.getValueNode();
                        switch (construct.apply(keyNode)) {
                            case String provider when "provider".equals(provider) ->
                                providerType = getProviderType(construct.apply(valueNode).toString());
                            case String providers when "providers".equals(providers) ->
                                providerSettings = getProviderSettings((Map<String, Object>) construct.apply(valueNode));
                            case String chatMemory when "chat-memory".equals(chatMemory) ->
                                chatMemorySettings = getChatMemorySettings((Map<String, Object>) construct.apply(valueNode));
                            default ->
                                throw new IllegalArgumentException("Unknown key: " + construct.apply(keyNode));

                        }

                    }
                    return new Config(providerType, providerSettings, chatMemorySettings);
                });

        var options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setEnumCaseSensitive(false);
        var constructor = new ConfigConstructor(Config.class, options, mappingNodeConstructors);
        var configDescription = new TypeDescription(Config.class);
        configDescription.addPropertyParameters("providers", Config.ProviderType.class, Config.ProviderSettings.class);
        constructor.addTypeDescription(configDescription);
        return constructor;
    }

    private Map<String, Object> loadYamlAsMap() {
        Yaml yaml = getYaml(new Constructor(new LoaderOptions()));
        yaml.setBeanAccess(org.yaml.snakeyaml.introspector.BeanAccess.FIELD);
        String filePath = getFilePath();
        if (filePath != null) {
            try (InputStream in = Files.newInputStream(Path.of(filePath))) {
                return yaml.load(in);
            } catch (IOException e) {
                logger.warn("Could not load config file", e);
            }
        }
        return new java.util.HashMap<>();
    }

    private void saveToYaml(Map<String, Object> props) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        try {
            Path configPath = getConfigPath();
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath);
            }
            Path configFile = configPath.resolve(CONFIG_FILE_NAME);
            try (Writer writer = Files.newBufferedWriter(configFile, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                yaml.dump(props, writer);
            }
        } catch (IOException e) {
            logger.warn("Could not save config file", e);
        }
    }

    private Path getConfigPath() throws IOException {
        return Path.of(configDirectory.getURI());
    }

    private void flattenMapRecursive(Map<String, Object> map, String prefix, Map<String, String> result) {
        if (map == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenMapRecursive(nestedMap, newKey, result);
            } else if (value != null) {
                result.put(newKey, value.toString());
            }
        }
    }

    /**
     * Transforms a flattened map with dot-notation keys back into a nested map
     * structure.
     * For example, a flattened structure like:
     * {
     * "provider": "ollama",
     * "providers.ollama.base-url": "http://localhost:11433"
     * }
     * 
     * Will be unflattened to:
     * {
     * "provider": "ollama",
     * "providers": {
     * "ollama": {
     * "base-url": "http://localhost:11433"
     * }
     * }
     * }
     * 
     * @param flattenedMap The flattened map with dot-notation keys
     * @return A nested map structure
     */
    public Map<String, Object> unflattenMap(Map<String, String> flattenedMap) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, String> entry : flattenedMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String[] parts = key.split("\\.");
            Map<String, Object> current = result;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) current.computeIfAbsent(part,
                        _ -> new HashMap<String, Object>());
                current = nestedMap;
            }

            current.put(parts[parts.length - 1], value);
        }

        return result;
    }

    /**
     * Flattens a nested map structure into a single-level map with dot-notation
     * keys.
     * For example, a nested structure like:
     * {
     * "provider": "ollama",
     * "providers": {
     * "ollama": {
     * "base-url": "http://localhost:11433"
     * }
     * }
     * }
     * 
     * Will be flattened to:
     * {
     * "provider": "ollama",
     * "providers.ollama.base-url": "http://localhost:11433"
     * }
     * 
     * @param map The nested map to flatten
     * @return A flattened map with dot-notation keys
     */
    public Map<String, String> flattenMap(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        flattenMapRecursive(map, "", result);
        return result;
    }

    public void init() {
        try {
            Path configPath = getConfigPath();
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath);
            }
            Path configFile = configPath.resolve(CONFIG_FILE_NAME);
            if (Files.notExists(configFile)) {
                Files.createFile(configFile);
            }
        } catch (IOException ex) {
            logger.warn("Failed to initialize config", ex);
        }
    }

    public Map<String, String> list() {
        Map<String, Object> map = loadYamlAsMap();
        return flattenMap(map);
    }

    public String get(String key) {
        Map<String, Object> map = loadYamlAsMap();
        Map<String, String> flattenedMap = flattenMap(map);
        return flattenedMap.get(key);
    }

    /**
     * Sets the configuration values.
     *
     * @param values The map of configuration values to set
     * @throws IllegalStateException    if the configuration file cannot be saved
     * @throws IllegalArgumentException if the configuration key is invalid
     */
    public void set(Map<String, String> values) {
        Map<String, Object> currentMap = loadYamlAsMap();
        Map<String, String> flattenedMap = flattenMap(currentMap);

        // Update the flattened map with the new values
        flattenedMap.putAll(values);

        // Convert back to nested structure and save
        Map<String, Object> updatedMap = unflattenMap(flattenedMap);
        saveToYaml(updatedMap);
        try {
            Config config = loadYamlAsBean();
            if (config != null && config.chatMemory() != null) {
                config.chatMemory().resolve(); // Validate the chat memory configuration
            }
        } catch (IllegalStateException ex) {
            if(logger.isErrorEnabled()) {
                logger.error("Failed to update configuration", ex);
            }
            // Rollback the changes if an error occurs
            unset(values.keySet().stream().toList());
            throw ex;
        } catch (ConstructorException ex) {
            if(logger.isErrorEnabled()) {
                logger.error("Failed to update configuration", ex);
            }
            // Rollback the changes if an error occurs
            unset(values.keySet().stream().toList());
            throw ex.getCause() instanceof IllegalArgumentException ia ? ia : new RuntimeException(ex);
        }
    }

    public void unset(List<String> keys) {
        Map<String, Object> currentMap = loadYamlAsMap();
        Map<String, String> flattenedMap = flattenMap(currentMap);

        // Remove the specified keys
        keys.forEach(flattenedMap::remove);

        // Convert back to nested structure and save
        Map<String, Object> updatedMap = unflattenMap(flattenedMap);
        saveToYaml(updatedMap);
    }

    String getDir() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath) && Files.isDirectory(configPath)) {
                return configPath.toAbsolutePath().toString();
            }
        } catch (IOException ex) {
            logger.warn("[{}] is not a valid directory", configDirectory, ex);
        }
        return null;
    }

    String getFilePath() {
        Path configFile = null;
        try {
            Path configPath = getConfigPath();
            configFile = configPath.resolve(CONFIG_FILE_NAME);
            if (Files.exists(configFile) && Files.isRegularFile(configFile)) {
                return configFile.toAbsolutePath().toString();
            }
        } catch (IOException ex) {
            logger.warn("[{}] is not a valid file", configFile, ex);
        }
        return null;
    }

}
