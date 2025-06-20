package org.simplecommerce.ai.commerce.rag;

import java.io.IOException;
import java.nio.file.Path;

import org.simplecommerce.ai.commerce.command.ChatbotVersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
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
                processFileTarget();
            } else {
                processVectorStoreTarget();
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

    private void processFileTarget() throws IOException {
        Path result = ragService.processToFile(document, outputFile);
        spec.commandLine().getOut().println("Processed document saved to: " + result);
    }

    private void processVectorStoreTarget() throws IOException {
        ragService.processToVectorStore(document);
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
