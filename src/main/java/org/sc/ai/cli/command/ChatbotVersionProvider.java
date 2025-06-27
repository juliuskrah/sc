package org.sc.ai.cli.command;

import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

import picocli.CommandLine.IVersionProvider;

public class ChatbotVersionProvider implements IVersionProvider {

    private static final String FORMAT_TWO_COLS = "%-24s%s";

    @Override
    public String[] getVersion() throws Exception {
        var resource = new ClassPathResource("META-INF/build-info.properties");
        var properties = new Properties();
        properties.load(resource.getInputStream());
        
        return new String[] {
            String.format(FORMAT_TWO_COLS, "sc", properties.getProperty("build.version", "unknown")),
            String.format(FORMAT_TWO_COLS, "Build Time", properties.getProperty("build.time", "unknown")),
            String.format(FORMAT_TWO_COLS, "JVM", "${java.version}"),
            String.format("%-24s%s %s %s", "OS", "${os.name}", "${os.version}", "${os.arch}")
        };
    }
}
