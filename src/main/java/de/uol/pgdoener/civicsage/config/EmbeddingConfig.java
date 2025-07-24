package de.uol.pgdoener.civicsage.config;

import com.knuddels.jtokkit.api.EncodingType;
import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmbeddingConfig {

    private static final double CONTEXT_WINDOW_FILLED_RATIO = 0.85;

    private final int contextLength;
    private final int documentContextLength;

    public EmbeddingConfig(AIProperties aiProperties) {
        this.contextLength = aiProperties.getEmbedding().getModel().getContextLength();
        this.documentContextLength = aiProperties.getEmbedding().getDocumentContextLength();
    }

    // https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_default_implementation
    @Bean
    public BatchingStrategy customTokenCountBatchingStrategy() {
        log.info("Initializing TokenCountBatchingStrategy with {} tokens", contextLength);
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,
                contextLength,
                0.1
        );
    }

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize((int) (documentContextLength * CONTEXT_WINDOW_FILLED_RATIO))
                .build();
    }

}
