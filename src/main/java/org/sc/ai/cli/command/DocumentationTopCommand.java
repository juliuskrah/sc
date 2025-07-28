package org.sc.ai.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Documentation-only version of TopCommand without Spring dependencies
 * for man page generation with enhanced details and examples.
 * 
 * @author Julius Krah
 */
@Command(name = "sc", description = {
        "A runtime for AI chatbots supporting multiple LLM providers.",
        """
                sc is a command-line interface for interacting with
                Large Language Models (LLMs) from various providers including Ollama and OpenAI.
                It provides chat capabilities, configuration management, and RAG
                (Retrieval-Augmented Generation) functionality.

                Examples::
                * sc chat "What is the weather today?"
                * sc chat -m llama3.2 "Explain quantum computing"
                * sc config --set providers.ollama.model=codellama
                * sc rag --etl=vectorStore file:///path/to/document.pdf
                """
}, mixinStandardHelpOptions = true, subcommands = {
        DocumentationChatCommand.class,
        DocumentationConfigCommand.class,
        CommandLine.HelpCommand.class,
        DocumentationRagCommand.class,
})
public class DocumentationTopCommand implements Runnable {

    @Option(names = { "--base-url" }, paramLabel = "BASE_URL", description = {
            "Ollama API endpoint URL.",
            "Overrides configuration file setting.",
            "Default: http://localhost:11434"
    })
    private String baseUrl;

    @Override
    public void run() {
        // Documentation only - no implementation needed
    }
}

@Command(name = "chat", description = {
        "Chat with a bot using various LLM providers.",
        "",
        "Start interactive or single-shot conversations with AI models.",
        "Supports multiple models from Ollama and other providers.",
        "",
        "REPL Mode:",
        "[source,bash]",
        "----",
        "  sc chat",
        "  sc> Hello, how are you?",
        "  sc> exit",
        "----",
        "Single-shot Mode:",
        "  sc chat \"What is the capital of France?\"",
        "",
        "Using Different Models:",
        "  sc chat -m llama3.2 \"Explain machine learning\"",
        "  sc chat --model codellama \"Write a Python function\"",
        "",
        "With Custom Endpoint:",
        "  sc --base-url http://192.168.1.100:11434 chat \"Hello\""
}, mixinStandardHelpOptions = true)
class DocumentationChatCommand implements Runnable {
    @Option(names = { "-m", "--model" }, paramLabel = "MODEL", description = {
            "Specify the LLM model to use for this conversation.",
            "Overrides the default model from configuration.",
            "Examples: llama3.2, codellama, mistral, phi3"
    })
    private String model;

    @Parameters(arity = "0..1", paramLabel = "MESSAGE", description = {
            "Optional message to send to the bot.",
            "If not provided, enters REPL mode.",
            "In REPL mode, type `/exit` or `/quit` or `/bye` to end the session."
    })
    private String message;

    @Override
    public void run() {
        // Documentation only
    }
}

@Command(name = "config", description = {
        "Manage configuration settings for the sc CLI.",
        """
                The configuration system supports hierarchical configuration loading:
                1. Command line options (highest priority)
                2. Configuration file at $HOME/.sc/config
                3. Default values (lowest priority)

                Configuration File Location:
                  - $HOME/.sc/config
                  - $SC_CONFIG_DIR/config

                Examples:
                  sc config init                                   # Initialize config file
                  sc config --get providers.ollama.model           # Get a specific setting
                  sc config --set providers.ollama.model=llama3.2  # Set a configuration value
                  sc config --file                                 # Show config file path
                  sc config --dir                                  # Show config directory
                """
}, mixinStandardHelpOptions = true, subcommands = { DocumentationConfigInitCommand.class })
class DocumentationConfigCommand implements Runnable {
    @Option(names = { "--dir" }, description = {
            "Display the configuration directory path.",
            "Shows where configuration files are stored."
    })
    private boolean dir;

    @Option(names = { "--file" }, description = {
            "Display the configuration file path.",
            "Shows the location of the active configuration file."
    })
    private boolean file;

    @Option(names = { "-g", "--get" }, paramLabel = "KEY", description = {
            "Get configuration value for the specified key.",
            "Supports dot notation for nested properties.",
            "Example: sc config --get providers.ollama.model"
    })
    private String get;

    @Option(names = { "-s", "--set" }, paramLabel = "KEY=VALUE", description = {
            "Set configuration key to value.",
            "Creates configuration file if it doesn't exist.",
            "Use dot notation for nested properties.",
            "Examples: sc config --set providers.ollama.model=llama3.2 --set provider=ollama"
    })
    private java.util.Map<String, String> set;

    @Option(names = { "-u", "--unset" }, paramLabel = "KEY", description = {
            "Remove configuration key and its value.",
            "Supports multiple keys separated by spaces.",
            "Example: sc config --unset providers.ollama.model --unset provider"
    })
    private java.util.List<String> unset;

    @Override
    public void run() {
        // Documentation only
    }
}

@Command(name = "init", description = {
        "Initialize the configuration file with default settings.",
        "",
        "Creates the configuration directory ($HOME/.sc) and configuration file ($HOME/.sc/config)",
        "if they don't exist. Sets up default configuration values for",
        "Ollama connection and other essential settings.",
        "",
        "Example:",
        "  sc config init",
        "",
        "This will create:",
        "  - Configuration directory: $HOME/.sc/",
        "  - Configuration file: $HOME/.sc/config",
        "  - Default Ollama endpoint: http://localhost:11434"
}, mixinStandardHelpOptions = true)
class DocumentationConfigInitCommand implements Runnable {
    @Override
    public void run() {
        // Documentation only
    }
}

@Command(name = "rag", description = {
        "Interact with the RAG (Retrieval-Augmented Generation) system.",
        """
                Process documents and perform ETL operations for enhanced AI responses.
                Supports local files and remote HTTPS resources with multiple formats
                including `PDF`, `Markdown`, `JSON`, and `HTML` documents.

                ETL Operations::
                * file        - Extract, process store documents in a txt file
                * vectorStore - Store document embeddings in a vector database

                Examples::
                * sc rag file:///home/user/document.pdf --output=summary.txt
                * sc rag https://example.com/article.html --etl=vectorStore
                * sc rag file:///docs/manual.md --output=summary.txt --etl=file

                Supported Document Types::
                * PDF files (.pdf)
                * Markdown files (.md, .markdown)
                * HTML web pages (.html, .htm)
                * JSON files (.json)
                * Plain text files (.txt)

                Supported Protocols::
                * file://  Local file system
                * https:// Remote HTTPS resources (secure only)
                * github:// GitHub repository files
                """
}, mixinStandardHelpOptions = true)
class DocumentationRagCommand implements Runnable {
    @Option(names = { "-o", "--output" }, paramLabel = "OUTPUT_FILE", description = {
            "Output filename for the RAG response.",
            "Must be used with '--etl=file' operation.",
            "Saves processed content to specified file.",
            "Example: --output=summary.txt"
    })
    private java.nio.file.Path outputFile;

    @Option(names = { "--etl" }, paramLabel = "TARGET", description = {
            "ETL operation target specifying how to process the document.",
            "* file        - Extract content and optionally save to output file",
            "* vectorStore - Process and store embeddings in a vector database",
            "Default: file"
    }, defaultValue = "file")
    private String etlTarget;

    @Parameters(paramLabel = "DOCUMENT", arity = "1", description = {
            "The document to process using one of the supported protocols:",
            "",
            "Local Files::",
            "* file:///absolute/path/to/document.pdf",
            "* file:///home/user/docs/manual.md",
            "",
            "GitHub Files::",
            "* github://user/repo/contents/path/to/file",
            "* github://user/repo/contents/path/to/another/file",
            "",
            "Remote Files (HTTPS only)::",
            "* https://example.com/documentation.html",
            "* https://github.com/user/repo/raw/main/README.md",
            "",
            "Note: Only HTTPS URLs are supported for security reasons."
    })
    private String document;

    @Override
    public void run() {
        // Documentation only
    }
}
