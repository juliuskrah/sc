# Introduction

`ai` is a cli application that makes it easy to interact with LLMs from various providers, including Ollama and OpenAI.

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
  - [logs](#logs)
    - [Options](#logs-options)
    - [Parameters](#logs-parameters)
    - [Usage](#logs-usage)
  - [ps](#ps)
    - [Usage](#ps-usage)
  - [rag](#rag)
    - [Options](#rag-options)
    - [Parameters](#rag-parameters)
    - [Usage](#rag-usage)
  - [serve](#serve)
    - [Options](#serve-options)
    - [Usage](#serve-usage)
  - [stop](#stop)
    - [Parameters](#stop-parameters)
    - [Usage](#stop-usage)
  - [Global Options](#global-options)
- [FAQ](#faq)
- [Development](#development)
  - [Lightweight Container with Cloud Native Buildpacks](#lightweight-container-with-cloud-native-buildpacks)
  - [Executable with Native Build Tools](#executable-with-native-build-tools)
  - [Gradle Toolchain support](#gradle-toolchain-support)

</details>

# Available Commands

## `chat`

This command allows you to chat with the Ollama API and other LLMs. You can use it to send messages and receive responses from the model.

`chat` context is implemented via:

- RAG
  - Documents are provided to the to the LLM through the following mechanisms:
    - `file:///path/to/file` - Local file
    - `https://<url>` - Remote file. Only https is supported.
    - `s3://<bucket>/<key>` - S3 file
- Tools
- Attachments

`chat` memory is supported via:

- RDBMS
- neo4j
- Cassandra

### Options <a name="chat-options"></a>

* `-m, --model`: Specify the model to use for the chat. <required>".
* `--base-url`: Ollama API endpoint. The default is http://localhost:11434".
* `-t, --temperature`: Specify the temperature for the model. The default is 0.3.

### Parameters <a name="chat-parameters"></a>

* `MESSAGE`: The prompt to send to the model.

### Usage <a name="chat-usage"></a>

```bash
sc chat --model llama3.2 --base-url http://localhost:11434 --temperature 0.3 "Hello, how are you?"
```

> Note: To use the `chat` command in interactive mode, you can omit the `MESSAGE` parameter. The command will then prompt you for input.

## `config`

This command allows you to view or set the configuration for the CLI. You can use it to manage settings such as the Ollama API endpoint and other CLI-specific configurations.

The configuration reference contains the following keys:

```yaml
# ~/.sc/config
provider: ollama # or openai
providers:
  ollama: # Ollama provider configuration
    base-url: http://localhost:11433
    model: qwen2:0.5b
  openai: # OpenAI provider configuration
    base-url: https://api.openai.com/v1
    model: gpt-3.5-turbo
    options: {} # provider-specific options
```

By default, the configuration file is stored in `$HOME/.sc/config`. You can change the configuration directory by setting the `SC_CONFIG_DIR` environment variable.

### Options <a name="config-options"></a>

* `--dir`: Show the configuration directory. This option will display the path to the directory where the configuration
  files are stored. The default directory is `$HOME/.sc/` however this can be overridden by setting the `SC_CONFIG_DIR` environment variable.
* `--file`: Show the configuration file. This option will display the path to the configuration file used by the CLI.
* `--set`: Set a configuration setting. You need to provide the key and value in the format `key=value`. Invalid keys will be ignored. This option also creates the configuration file if it does not exist. The configuration file is stored in the directory specified by `--dir` or the default directory if not specified. The default configuration file is `$HOME/.sc/config`.
* `--get`: Get the value of a specific configuration setting. You need to provide the key.
* `--unset`: Unset a configuration setting. You need to provide the key.

### Usage <a name="config-usage"></a>

View the configuration directory:

```bash
sc config --dir
```

View the configuration file:

```bash
sc config --file
```

Set configuration properties. This option also creates the configuration file if it does not exist:

```bash
sc config --set providers.ollama.base-url=http://localhost:11434 --set provider=ollama
```

Get a configuration property:

```bash
sc config --get providers.ollama.base-url
```

Unset/remove a configuration properties:

```bash
sc config --unset ollama.baseUrl --unset provider
```

## `config init`

This command initializes the configuration file for the CLI. It creates a default configuration file if it does not already exist. This is useful for setting up the CLI for the first time or resetting the configuration. When you run this command, it will create a configuration file in the default directory (`$HOME/.sc/config`) with the default settings.

> [!NOTE]
> If the configuration file already exists, this command will not overwrite it. It will only create the file if it does not exist. You can override the default configuration directory by setting the `SC_CONFIG_DIR` environment variable.

### Usage <a name="config-init-usage"></a>

```bash
sc config init
```

## `logs`

This command allows you to view the logs of a service. This is especially useful with the `serve` command in detached mode, as it
provides real-time updates on the service's status and any errors that may occur.

### Options <a name="logs-options"></a>

* `-f, --follow`: Follow the logs in real-time. This option is useful for monitoring the service as it runs.

### Parameters <a name="logs-parameters"></a>

* `SERVICE`: The name or ID of the service to view logs for.

### Usage <a name="logs-usage"></a>

```bash
sc logs --follow my-service
```

## `ps`

This command allows you to view the running services in the application. This is especially useful for monitoring the application's resource usage and performance.

### Usage <a name="ps-usage"></a>

```bash
sc ps
```

## `rag`

This command allows you to interact with the RAG (Retrieval-Augmented Generation) system. It provides options to manage documents and attachments.

### Options <a name="rag-options"></a>

* `-o, --output`: Specify output filename for the RAG response. This option must be used with the `--etl=file` option.
* `--etl`: Specify the ETL (Extract, Transform, Load) operation target. The available targets are:
  - `file`: Write output to a file from the local filesystem (default).
  - `vectorStore`: Write output to a vector store.

### Parameters <a name="rag-parameters"></a>

* `DOCUMENT`: The document to process. This can be a local file, a remote file (HTTPS), or a cloud storage file (S3, GCS, Azure).
  The following protocols are supported:
  - `file:///path/to/file`: Local file
  - `https://<url>`: Remote file (only HTTPS is supported)
  - `s3://<bucket>/<key>`: S3 file (Planned for future support)

### Usage <a name="rag-usage"></a>

```bash
sc rag --etl=file --output output.txt file:///path/to/document.pdf
```

## `serve`

This command allows you to start a web service.

### Options <a name="serve-options"></a>
* `-d, --detach`: Run a service in the background and print service ID.
* `-p, --port`: Specify the port for the application. The default is 8080.
* `--name`: Assign a name to the service.

### Usage <a name="serve-usage"></a>

```bash
sc serve --detach --port 8080 --name my-service
```

## `stop`

This command allows you to stop a running service.

### Parameters <a name="stop-parameters"></a>

* `SERVICE`: The name or ID of the service to stop.

### Usage <a name="stop-usage"></a>

```bash
sc stop my-service
```

## Global Options

* `-h, --help`: Show help message and exit.
* `-v, --version`: Show the version of the CLI.
* `--context`: Specify the context to use.

# FAQ

## How do I enable logging?

You can enable logging with the following environment variable:

```bash
JAVA_TOOL_OPTIONS=-Dlogging.level.org.simplecommerce.ai.commerce=debug sc <args>
```

# Development

## Lightweight Container with Cloud Native Buildpacks
If you're already familiar with Spring Boot container images support, this is the easiest way to get started.
Docker should be installed and configured on your machine prior to creating the image.

To create the image, run the following goal:

```bash
./gradlew bootBuildImage
```

Then, you can run the app like any other container:

```bash
docker run --rm commerce-projects/sc:0.0.1-SNAPSHOT
```

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

## Gradle Toolchain support

There are some limitations regarding Native Build Tools and Gradle toolchains.
Native Build Tools disable toolchain support by default.
Effectively, native image compilation is done with the JDK used to execute Gradle.
You can read more about [toolchain support in the Native Build Tools here](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html#configuration-toolchains).
