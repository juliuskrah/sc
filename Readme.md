# Introduction

Simple Commerce is a cli application that makes it easy to interact with the Ollama API and other LLMs.

<details>
<summary><strong>Table of Contents</strong></summary>

- [Available Commands](#available-commands)
  - [chat](#chat)
    - [Options](#chat-options)
    - [Parameters](#chat-parameters)
    - [Usage](#chat-usage)
  - [logs](#logs)
    - [Options](#logs-options)
    - [Parameters](#logs-parameters)
    - [Usage](#logs-usage)
  - [ps](#ps)
    - [Usage](#ps-usage)
  - [serve](#serve)
    - [Options](#serve-options)
    - [Usage](#serve-usage)
  - [stop](#stop)
    - [Parameters](#stop-parameters)
    - [Usage](#stop-usage)
- [Development](#development)
  - [Lightweight Container with Cloud Native Buildpacks](#lightweight-container-with-cloud-native-buildpacks)
  - [Executable with Native Build Tools](#executable-with-native-build-tools)
  - [Gradle Toolchain support](#gradle-toolchain-support)

</details>

# Available Commands

## `chat`

This command allows you to chat with the Ollama API and other LLMs. You can use it to send messages and receive responses from the model.

`chat` context is supported.
- RAG
- Memory
- Vector DB
- Embeddings
- Files can be to provide context to the model. Workds starting with `@<protocol>:///path/to/file` are treated as files.
  - `@file:///path/to/file` - Local file
  - `@http://<url>` - Remote file
  - `@https://<url>` - Remote file
  - `@s3://<bucket>/<path>` - S3 file
  - `@gcs://<bucket>/<path>` - GCS file
  - `@azure://<container>/<path>` - Azure file

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

## `logs`

This command allows you to view the logs of a service. This is especially useful with the `serve` command in detached mode, as it
provides real-time updates on the service's status and any errors that may occur.

### Options

* `-f, --follow`: Follow the logs in real-time. This option is useful for monitoring the service as it runs.

### Parameters

* `SERVICE`: The name or ID of the service to view logs for.

### Usage

```bash
sc logs --follow my-service
```

## `ps`

This command allows you to view the running services in the application. This is especially useful for monitoring the application's resource usage and performance.

### Usage

```bash
sc ps
```

## `serve`

This command allows you to start a web service.

### Options
* `-d, --detach`: Run a service in the background and print service ID.
* `-p, --port`: Specify the port for the application. The default is 8080.
* `--name`: Assign a name to the service.

### Usage

```bash
sc serve --detach --port 8080 --name my-service
```

## `stop`

This command allows you to stop a running service.

### Parameters

* `SERVICE`: The name or ID of the service to stop.

### Usage

```bash
sc stop my-service
```

# FAQ

## How do I enable logging?

You can enable logging with the following environment variable:

```bash
export JAVA_TOOL_OPTIONS=-Dlogging.level.org.simplecommerce.ai.commerce=debug
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
docker run --rm commerce-projects:sc:0.0.1-SNAPSHOT
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
