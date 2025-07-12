sc CLI

# General instructions

- Generate tests for generated code.

## Git instructions

- Prefer conventional commits for commit messages.

## PR instructions

- For longer PR descriptions, use the `create_pull_request` tool if available.

## Folder structure

```plaintext
.
├── .github 
│   ├── instructions
│   │   ├── gradle-build.instructions.md
│   │   └── java-project.instructions.md
│   ├── copilot-instructions.md
│   └── workflows
│       ├── ci.yml
│       └── deploy-docs.yml
├── .gitignore
├── .gitattributes
├── .nojekyll
├── .sc/
├── .vscode/
├── Readme.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── migrate-packages.sh
├── rewrite.yml
├── settings.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src
    ├── docs
    │   └── man-templates
    │       ├── sc-chat.adoc
    │       ├── sc-config.adoc
    │       ├── sc-config-init.adoc
    │       ├── sc-help.adoc
    │       ├── sc-rag.adoc
    │       └── sc.adoc
    ├── main
    │   ├── java
    │   │   └── org
    │   │       └── sc
    │   │           └── ai
    │   │               └── cli
    │   │                   ├── chat/          # Interactive chat functionality with AI models
    │   │                   ├── command/       # CLI command definitions and version providers
    │   │                   ├── config/        # Configuration management and initialization
    │   │                   └── rag/           # Retrieval-Augmented Generation document processing
    │   └── resources
    │       ├── application.properties
    │       └── META-INF/
    └── test
        ├── java
        │   └── org
        │       └── sc
        │           └── ai
        │               └── cli
        │                   ├── chat/          # Tests for chat functionality
        │                   ├── command/       # Tests for CLI commands
        │                   ├── config/        # Tests for configuration management
        │                   └── rag/           # Tests for RAG functionality
        └── resources
            └── application-test.yaml
```
