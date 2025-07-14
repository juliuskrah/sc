package org.sc.ai.cli.rag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.sc.ai.cli.command.ChatbotVersionProvider;
import org.sc.ai.cli.command.ProviderMixin;
import org.sc.ai.cli.command.Spinner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Command for interacting with the RAG (Retrieval-Augmented Generation) system.
 * 
 * @author Julius Krah
 */
@Component
@Command(name = "rag", description = "Interact with the RAG (Retrieval-Augmented Generation) system", mixinStandardHelpOptions = true, versionProvider = ChatbotVersionProvider.class, subcommands = CommandLine.HelpCommand.class)
public class RagCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RagCommand.class);
    private final RagService ragService;
    
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private ProviderMixin providerMixin;

    @Option(names = { "-o", "--output" }, paramLabel = "OUTPUT_FILE", description = "Output filename for the RAG response. Must be used with '--etl=file'")
    private Path outputFile;

    @Option(names = "--etl", paramLabel = "TARGET", description = "ETL operation target: ${COMPLETION-CANDIDATES}. Default: ${DEFAULT-VALUE}", defaultValue = "file")
    private EtlTarget etlTarget;

    @Parameters(paramLabel = "DOCUMENT", arity = "1", description = """
            The document to process. Supported protocols:
            - @|bg(cyan) file:///path/to/file|@ (Local file)
            - @|bg(cyan) https://<url>|@        (Remote file, HTTPS only)
            - @|bg(cyan) s3://<bucket>/<key>|@  (S3 file)""")
    private String document;

    public RagCommand(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public void run() {
        validateParameters();
        try {
            if (etlTarget == EtlTarget.FILE) {
                processFileTarget(Spinner::stop);
            } else {
                processVectorStoreTarget(Spinner::stop);
            }
        } catch (IOException e) {
            if(logger.isErrorEnabled()) {
                logger.error("Error processing document: {}", e.getMessage(), e);
            }
            throw new CommandLine.ExecutionException(spec.commandLine(), "Failed to process document: " + e.getMessage());
        }
    }

    private void validateParameters() {
        if (outputFile != null && etlTarget != EtlTarget.FILE) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "The --output option can only be used with '--etl=file'");
        }
        if (outputFile == null && etlTarget == EtlTarget.FILE) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "The --output option is required when using '--etl=file'");
        }
    }

    private void processFileTarget(Consumer<Spinner> callback) throws IOException {
        var spinner = new Spinner(spec.commandLine().getOut(), "Processing...");
        spinner.start();
        Path result = ragService.processToFile(document, outputFile);
        callback.accept(spinner);
        spec.commandLine().getOut().println("Processed document saved to: " + result);
    }

    private void processVectorStoreTarget(Consumer<Spinner> callback) throws IOException {
        var spinner = new Spinner(spec.commandLine().getOut(), "Processing...");
        spinner.start();
        ragService.processToVectorStore(document);
        callback.accept(spinner);
        spec.commandLine().getOut().println("Document processed and saved to vector store");
    }

    enum EtlTarget {
        FILE("file"),
        VECTOR_STORE("vectorStore");

        private final String name;

        EtlTarget(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
