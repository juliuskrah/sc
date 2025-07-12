module org.sc.ai.cli {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires org.slf4j;
    requires org.jline;
    requires info.picocli;
    requires org.yaml.snakeyaml;
    requires reactor.core;
    requires jakarta.annotation;
    // Spring AI modules
    requires spring.ai.starter.model.ollama;
    requires spring.ai.starter.model.chat.memory.repository.jdbc;
    requires spring.ai.pdf.document.reader;
    requires spring.ai.markdown.document.reader;
    requires spring.ai.jsoup.document.reader;

    opens org.sc.ai.cli to spring.core, spring.beans, spring.context;
    opens org.sc.ai.cli.chat to spring.core, spring.beans, spring.context;
    opens org.sc.ai.cli.config to spring.core, spring.beans, spring.context;
    opens org.sc.ai.cli.rag to spring.core, spring.beans, spring.context;
    opens org.sc.ai.cli.command to spring.core, spring.beans, spring.context;

    exports org.sc.ai.cli;
    exports org.sc.ai.cli.chat;
    exports org.sc.ai.cli.config;
    exports org.sc.ai.cli.rag;
    exports org.sc.ai.cli.command;
}
