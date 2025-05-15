package com.simplecommerce.ai.commerce.command;

import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

import picocli.CommandLine.IVersionProvider;

public class CommerceVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        var resource = new ClassPathResource("META-INF/build-info.properties");
        var properties = new Properties();
        properties.load(resource.getInputStream());
        
        return new String[] {
            String.format("Simple Commerce \t%s", properties.getProperty("build.version", "unknown")),
            String.format("Build Time \t\t%s", properties.getProperty("build.time", "unknown")),
            "JVM \t\t\t${java.version}",
            "OS \t\t\t${os.name} ${os.version} ${os.arch}",
        };
    }
}
