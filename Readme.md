# Introduction

`sc` is a cli application that makes it easy to interact with LLMs from multiple providers, including Ollama and OpenAI.

## Installation

### Download Installers

Download the latest release installer for your platform from the [GitHub releases page](https://github.com/juliuskrah/sc/releases).

Available installers:
- **Linux**: .deb (Debian/Ubuntu), .rpm (RedHat/Fedora/SUSE) packages for x86_64
- **macOS**: .dmg, .pkg installers for x86_64 (Intel) and aarch_64 (Apple Silicon)  
- **Windows**: .exe, .msi installers for x86_64

### Package Managers

Coming soon:
- Homebrew (macOS/Linux)
- Scoop (Windows)
- Chocolatey (Windows)

### Build from Source

Requires Java 22+ (JDK 16+ for JPackage):

```bash
git clone https://github.com/juliuskrah/sc.git
cd sc
./gradlew bootJar
./gradlew jreleaserAssemble --assembler=jpackage
```

The installer will be available in `build/jreleaser/assemble/sc/jpackage/`.

## Release Information

This project uses automated releases with semantic versioning. See [RELEASE.md](RELEASE.md) for detailed information about the release process.

Current release stage: **ALPHA** - The project is in active development.

<details>
<summary><strong>Table of Contents</strong></summary>

- [Available Commands](#available-commands)
  - [chat](#chat)
    - [Options](#chat-options)
    - [Parameters](#chat-parameters)
    - [Usage](#chat-usage)
  - [config](#config)
    - [Options](#config-options)
    - [Usage](#config-usage)
  - [config init](#config-init)
    - [Usage](#config-init-usage)
  - [rag](#rag)
    - [Options](#rag-options)
    - [Parameters](#rag-parameters)
    - [Usage](#rag-usage)
  - [Global Options](#global-options)
- [FAQ](#faq)
- [Development](#development)
  - [Executable with Native Build Tools](#executable-with-native-build-tools)
  - [Future Work](#future-work)

</details>

# Available Commands

## `chat`

This command allows you to chat with the Ollama API and other LLMs. You can use it to send messages and receive responses from the model.

`chat` memory is implemented with HSQLDB, which is an in-memory database. You can find the database files in $HOME/.sc/store.db.*. See the [config](#config) command for more information on how to configure the chat memory.

### Options <a name="chat-options"></a>

* `-m, --model`: Specify the model to use for the chat. Currently only Ollama models are supported. The default is `mistral-small3.1`.
  > NOTE: The model should be available locally in the Ollama environment. You can list available models using the `ollama list` command.
* `--base-url`: Ollama API endpoint. The default is http://localhost:11434.

### Parameters <a name="chat-parameters"></a>

* `MESSAGE`: The prompt to send to the model.

### Usage <a name="chat-usage"></a>

```bash
sc chat --model llama3.2 --base-url=http://localhost:11434 "Hello, how are you?"
```

> Note: When you omit the `MESSAGE` parameter, this will start a REPL session.

```bash
sc chat
sc> Hello, how are you?
Hello! I'm functioning as intended, thank you. How can I assist you today?
```
Press `Ctrl+C` during a streamed response to cancel generation and return to the prompt.

## `config`

This command allows you to view or set the configuration for the CLI. You can use it to manage settings such as the Ollama API endpoint and other CLI-specific configurations.

The configuration [schema](./.sc/schema.json) will look something like this:

```yaml
# ~/.sc/config
provider: ollama # or openai
providers:
  ollama: # Ollama provider configuration
    base-url: http://localhost:11433
    model: qwen2:0.5b
  openai: # OpenAI provider configuration - see future work below
    base-url: https://api.openai.com/v1
    model: gpt-3.5-turbo
    options: {} # provider-specific options
chat-memory: # Chat memory configuration e.g. jdbc for HSQLDB
  jdbc:
    url: jdbc:hsqldb:mem:testdb
    username: sa
```

By default, the configuration file is stored in `$HOME/.sc/config`. You can change the configuration directory by setting the `SC_CONFIG_DIR` environment variable.

### Options <a name="config-options"></a>

* `--dir`: Show the configuration directory. This option will display the path to the directory where the configuration
  files are stored. The default directory is `$HOME/.sc/` however this can be overridden by setting the `SC_CONFIG_DIR` environment variable.
* `--file`: Show the configuration file. This option will display the path to the configuration file used by the CLI.
* `--set`: Set a configuration option. You need to provide the key and value in the format `key=value`. This option also creates the configuration file if it does not exist. You can view the location of the config directory with `--dir` option. The default configuration file is `$HOME/.sc/config`.
* `--get`: Get the value of a specific configuration option. You need to specify the key.
* `--unset`: Unset a configuration option. You need to specify the key.

### Usage <a name="config-usage"></a>

View the configuration directory:

```bash
sc config --dir
```

View the configuration file:

```bash
sc config --file
```

Set configuration options. This creates the configuration file if it does not exist:

```bash
sc config --set providers.ollama.base-url=http://localhost:11434 --set provider=ollama
```

Get a configuration option value:

```bash
sc config --get providers.ollama.base-url
```

Unset/remove a configuration properties:

```bash
sc config --unset providers.ollama.base-url --unset provider
```

## `config init`

This command initializes the configuration file for the CLI. It creates a default configuration file if it does not already exist. This is useful when setting up the CLI for the first time or resetting the configuration. When you run this command, it will create a configuration file in the default directory (`$HOME/.sc/`) with the default settings.

> [!NOTE]
> If the configuration file already exists, this command will not overwrite it. It will only create the file if it does not exist. You can override the default configuration directory by setting the `SC_CONFIG_DIR` environment variable.

### Usage <a name="config-init-usage"></a>

```bash
sc config init
```

## `rag`

This command allows you to interact with the RAG (Retrieval-Augmented Generation) system. You can load documents into a vector database or dump the RAG response to a file (dumping to file is only useful for testing and debugging). This is useful for processing documents and generating responses based on the content of those documents.

### Options <a name="rag-options"></a>

* `-o, --output`: Specify output filename for the RAG response. This option must be used in conjunction with the `--etl=file` option.
* `--etl`: Specify the ETL (Extract, Transform, Load) operation target. The available targets are:
  - `file`: Write output to a file from the local filesystem (default).

### Parameters <a name="rag-parameters"></a>

* `DOCUMENT`: The document to process. This can be a local document, a remote document (HTTPS), or a cloud document (S3, GCS, Azure).
  Documents can be loaded from various sources; the following protocols are supported when loading documents:
  - `file:///path/to/file`: Local document (absolute path)
  - `https://path/to/page`: Remote document (only HTTPS is supported)

The following document formats are supported:
  - `application/pdf`: PDF files
  - `text/html`: HTML files
  - `text/plain`: Plain text files
  - `text/markdown`: Markdown files (e.g. `.md` files)
  - `application/json`: JSON files

### Usage <a name="rag-usage"></a>

```bash
# Load a PDF document from the local filesystem and write the response to a file
sc rag --etl=file --output output.txt file:///path/to/document.pdf
# Load a HTML document from a remote URL and write the response to a file
sc rag --etl=file --output output.txt https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html
# Load a plain text document from a local file and write the response to a file
sc rag --etl=file --output output.txt file:///path/to/plain.txt
# Load a Markdown document from a remote URL and write the response to a file
sc rag --etl=file --output output.txt https://raw.githubusercontent.com/juliuskrah/quartz-manager/refs/heads/master/README.md
# Load a JSON document from the local filesystem and write the response to a file
sc rag --etl=file --output output.txt file:///path/to/document.json
```

## Global Options

* `-h, --help`: Show help message and exit.
* `-v, --version`: Show the version of the CLI.
* `--base-url`: Specify the base-url to use.

# FAQ

## How do I enable logging?

You can enable logging with the following environment variable:

```bash
JAVA_TOOL_OPTIONS=-Dlogging.level.org.sc.ai.cli=debug sc <args>
```

# Development

## Executable with Native Build Tools
Use this option if you want to explore more options such as running your tests in a native image.
The GraalVM `native-image` compiler should be installed and configured on your machine.

NOTE: GraalVM 22.3+ is required.

To create the executable, run the following goal:

```bash
./gradlew nativeCompile
```

Then, you can run the app as follows:

```bash
build/native/nativeCompile/sc --help
```

You can also run your existing tests suite in a native image.
This is an efficient way to validate the compatibility of your application.

To run your existing tests in a native image, run the following goal:

```bash
./gradlew nativeTest
```

## Local Documentation

For local documentation development, you can serve the docs locally:
```bash
# Generate docs
./gradlew generateDocs

# Serve locally (if you have Python installed)
cd build/docs && python -m http.server 8000
# Then visit http://localhost:8000
```

# Future Work

- `chat`: Attach files when chatting with the model. Support the following document types:
  - `image/*` - Image files
  - `text/*` - Text files
  - `application/pdf` - PDF files
  - `application/vnd.openxmlformats-officedocument.wordprocessingml.document` - Word documents
  - `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` - Excel spreadsheets
  - `application/vnd.openxmlformats-officedocument.presentationml.presentation` - PowerPoint presentations
  - `application/zip` - ZIP files
  - `application/json` - JSON files
- `chat memory`: Support for different memory backends:
  - `rdbms`: Relational database management system (e.g. PostgreSQL, MySQL)
  - `neo4j`: Neo4j graph database
  - `cassandra`: Apache Cassandra
- `chat LLM`: Support for different LLM providers:
  - `google`: Google Gemini API
  - `openai`: OpenAI API
- `chat agent`: Implement an agent that can perform tasks based on user input and context.
  - `tools`: Tool calling support for the agent.
  - `mcp`: Model context protocol support for the agent.
- `chat rag`: Leverage RAG (Retrieval-Augmented Generation) to enhance the chat experience by retrieving relevant documents and information from a vector store.
- `rag`: Document sources from:
    - `s3://<bucket>/<key>` - S3 document
    - `gcs://<bucket>/<key>` - GCS document
    - `azure://<container>/<blob>` - Azure Blob Storage document
    - `github://<owner>/<repo>/<path>` - GitHub document
    - `gitlab://<owner>/<repo>/<path>` - GitLab document
- `rag vectorStore`: Support for different vector databases:
    - `--etl=vectorStore`: Write output to a vector store. Used in a rag system to store and retrieve documents.

# Working with Documentation

This documentation for this project is generated using picocli's ManPageGenerator with template support for enhanced documentation.

## Generating Documentation

The documentation system supports two modes:

### Standard Generation
Automatically generates documentation from command annotations:
```bash
./gradlew generateDocs
```

### Template-Enhanced Generation  
Uses customizable templates for richer documentation content:
```bash
# First time only: Generate initial templates
./gradlew generateManpageTemplates

# Then use enhanced documentation generation
./gradlew generateDocs
```

## Template System

The template system allows for customization of the generated documentation while preserving the automatically generated command information.

### Template Structure

Templates are stored in `src/docs/man-templates/` and use AsciiDoctor's include mechanism:

```asciidoc
// Main template file example
:includedir: ../../../build/generated-picocli-docs

include::{includedir}/sc.adoc[tag=picocli-generated-man-section-header]
include::{includedir}/sc.adoc[tag=picocli-generated-man-section-name]  
include::{includedir}/sc.adoc[tag=picocli-generated-man-section-synopsis]

// Add custom sections here
== Getting Started
This section provides additional context...

== Examples  
Additional examples beyond what's auto-generated...

// Continue with generated sections
include::{includedir}/sc.adoc[tag=picocli-generated-man-section-options]
```

### Template Features

Each template includes:

- **Custom sections** with detailed explanations between generated content
- **Enhanced examples** and real-world use cases
- **Configuration details** and troubleshooting guides
- **Cross-references** to related commands and concepts
- **Additional context** not available in code annotations alone

## Configuration Schema

The project includes a JSON Schema for configuration validation:
- **Local**: `.sc/schema.json`
- **Published**: [`https://juliuskrah.com/sc/schemas/schema.json`](https://juliuskrah.com/sc/schemas/schema.json)

The schema is automatically included in the GitHub Pages deployment and can be used for:
- IDE autocomplete and validation in configuration files
- Configuration validation in external tools
- API documentation for configuration structure

## Documentation Tasks

Available Gradle tasks for documentation:

```bash
# Generate enhanced documentation (uses templates if available)
./gradlew generateDocs

# Generate initial templates (run only once)
./gradlew generateManpageTemplates

# Individual tasks
./gradlew generateEnhancedDocs  # Generate AsciiDoc with template support
./gradlew asciidoctor           # Convert AsciiDoc to HTML
```

The build system automatically detects whether templates exist and uses them for enhanced output, or falls back to standard generation.
