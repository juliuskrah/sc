package org.simplecommerce.ai.commerce.command;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec.Target;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;

/**
 * @author Julius Krah
 */
public class OllamaMixin {
    @Spec(Target.MIXEE)
    private CommandSpec mixee;
    @SuppressWarnings("unused")
    private String baseUrl;
    private static final Logger logger = LoggerFactory.getLogger(OllamaMixin.class);
    
    private static OllamaMixin topLevelOllamaMixin(CommandSpec commandSpec) {
        return ((TopCommand) commandSpec.root().userObject()).ollamaMixin;
    }

    private void init() {
        logger.debug("Using Ollama base-url: {}", getBaseUrl());
    }
    
    public String getBaseUrl() {
        return ((TopCommand) mixee.root().userObject()).getEndpoint();
    }
    
    @Option(names = { "--base-url" }, paramLabel = "BASE_URL", description = "Ollama API endpoint")
    public void setBaseUrl(String baseUrl) {
        topLevelOllamaMixin(mixee).baseUrl = baseUrl;
    }

    public static int executionStrategy(ParseResult parseResult) {
        topLevelOllamaMixin(parseResult.commandSpec()).init();
        return new CommandLine.RunLast().execute(parseResult);
    }
}
