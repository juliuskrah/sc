package org.simplecommerce.ai.commerce.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Julius Krah
 */
@Service
public class ConfigService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigService.class);
    @Value("${sc.config.dir:}")
    private Resource configDirectory;
    private static final String CONFIG_FILE_NAME = "config";

    private Path getConfigPath() throws IOException {
        return Path.of(configDirectory.getURI());
    }

    public void init() {
        try {
            Path configPath = getConfigPath();
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
            }
            Path configFile = configPath.resolve(CONFIG_FILE_NAME);
            if (!Files.exists(configFile)) {
                Files.createFile(configFile);
            }
        } catch (IOException ex) {
            logger.warn("Failed to initialize config", ex);
        }
    }

    public String get(String key) {
        Map<String, Object> props = loadProperties();
        Object value = props.get(key);
        if (value instanceof Map) {
            return new Yaml().dump(value);
        }
        return value != null ? value.toString() : null;
    }

    public void set(Map<String, String> values) {
        Map<String, Object> props = loadProperties();
        props.putAll(values);
        saveProperties(props);
    }

    public void unset(List<String> keys) {
        Map<String, Object> props = loadProperties();
        keys.forEach(props::remove);
        saveProperties(props);
    }

    public Map<String, Object> list() {
        return loadProperties();
    }

    private Map<String, Object> loadProperties() {
        Yaml yaml = new Yaml();
        String filePath = getFilePath();
        if (filePath != null) {
            try (InputStream in = Files.newInputStream(Path.of(filePath))) {
                Map<String, Object> data = yaml.load(in);
                return data != null ? data : new HashMap<>();
            } catch (IOException e) {
                logger.warn("Could not load config file", e);
            }
        }
        return new HashMap<>();
    }

    private void saveProperties(Map<String, Object> props) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        try {
            Path configPath = getConfigPath();
            if (!Files.exists(configPath)) {
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
