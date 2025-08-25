package org.sc.ai.cli.command;

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
public class ProviderMixin {
    @Spec(Target.MIXEE)
    private CommandSpec mixee;
    @SuppressWarnings("unused")
    private String baseUrl;
    private static final Logger logger = LoggerFactory.getLogger(ProviderMixin.class);
    
    private static ProviderMixin topLevelProviderMixin(CommandSpec commandSpec) {
        return ((TopCommand) commandSpec.root().userObject()).providerMixin;
    }

    private void init() {
        logger.debug("Using provider base-url: {}", getBaseUrl());
    }
    
    public String getBaseUrl() {
        return ((TopCommand) mixee.root().userObject()).getEndpoint();
    }
    
    @Option(names = { "--base-url" }, paramLabel = "BASE_URL", description = "Provider API endpoint")
    public void setBaseUrl(String baseUrl) {
        topLevelProviderMixin(mixee).baseUrl = baseUrl;
    }

    public static int executionStrategy(ParseResult parseResult) {
        topLevelProviderMixin(parseResult.commandSpec()).init();
        return new CommandLine.RunLast().execute(parseResult);
    }
}
