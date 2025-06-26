package org.simplecommerce.ai.commerce.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Documentation-only version of TopCommand without Spring dependencies
 * for man page generation.
 */
@Command(
    name = "sc",
    description = "A runtime for AI chatbots",
    mixinStandardHelpOptions = true,
    subcommands = {
        DocumentationChatCommand.class,
        DocumentationConfigCommand.class,
        CommandLine.HelpCommand.class,
        DocumentationRagCommand.class,
    }
)
public class DocumentationTopCommand implements Runnable {

    @Override
    public void run() {
        // Documentation only - no implementation needed
    }
}

@Command(name = "chat", description = "Chat with a bot", mixinStandardHelpOptions = true)
class DocumentationChatCommand implements Runnable {
    @Option(names = {"-m", "--model"}, paramLabel = "MODEL", description = "Specify LLM to use")
    private String model;
    
    @Parameters(arity = "0..1", paramLabel = "MESSAGE", description = "Message to send to the bot")
    private String message;

    @Override
    public void run() {
        // Documentation only
    }
}

@Command(name = "config", description = "Manage configuration settings", mixinStandardHelpOptions = true)
class DocumentationConfigCommand implements Runnable {
    @Option(names = {"-l", "--list"}, description = "List all configuration settings")
    private boolean list;
    
    @Option(names = {"-g", "--get"}, paramLabel = "KEY", description = "Get configuration value for the specified key")
    private String getValue;
    
    @Option(names = {"-s", "--set"}, paramLabel = "KEY=VALUE", description = "Set configuration key to value")
    private String setValue;
    
    @Option(names = {"-u", "--unset"}, paramLabel = "KEY", description = "Remove configuration key")
    private String unsetValue;

    @Override
    public void run() {
        // Documentation only
    }
}

@Command(name = "rag", description = "Interact with the RAG (Retrieval-Augmented Generation) system", mixinStandardHelpOptions = true)
class DocumentationRagCommand implements Runnable {
    @Option(names = {"-o", "--output"}, paramLabel = "OUTPUT_FILE", description = "Output filename for the RAG response. Must be used with '--etl=file'")
    private java.nio.file.Path outputFile;
    
    @Option(names = {"--etl"}, paramLabel = "TARGET", description = "ETL operation target: file, vector. Default: file", defaultValue = "file")
    private String etlTarget;
    
    @Parameters(paramLabel = "DOCUMENT", arity = "1", description = {
        "The document to process. Supported protocols:",
        "- file:///path/to/file (Local file)",
        "- https://<url>        (Remote file, HTTPS only)"
    })
    private String document;

    @Override
    public void run() {
        // Documentation only
    }
}
