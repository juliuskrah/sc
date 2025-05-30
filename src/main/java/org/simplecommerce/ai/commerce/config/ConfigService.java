package org.simplecommerce.ai.commerce.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author Julius Krah
 */
@Service
public class ConfigService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigService.class);
    @Value("${sc.config.dir:}")
    private Resource configDirectory;
    private static final String CONFIG_FILE_NAME = "config";

    String getDir() {
        try {
            if (configDirectory.exists() && configDirectory.getFile().isDirectory()) {
                return configDirectory.getFile().getAbsolutePath();
            }
        } catch (IOException ex) {
            logger.warn("[{}] is not a valid directory", configDirectory, ex);
        }
        return null;
    }

    String getFilePath() {
        Path configFile = null;
        try {
            configFile = configDirectory.getFile().toPath().resolve(CONFIG_FILE_NAME);
            if (Files.exists(configFile) && Files.isRegularFile(configFile)) {
                return configFile.toAbsolutePath().toString();
            }
        } catch (IOException ex) {
            logger.warn("[{}] is not a valid file", configFile, ex);
        }
        return null;
    }

}
