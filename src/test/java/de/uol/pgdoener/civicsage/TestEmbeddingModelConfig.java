package de.uol.pgdoener.civicsage;

import io.micrometer.common.lang.NonNullApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Objects;
import java.util.Random;

/**
 * This configuration provides a dummy embedding model for testing purposes.
 * The dummy model generates pseudo-random embeddings based on the document text.
 * This allows for consistent and repeatable tests without relying on an external embedding service.
 * <p>
 * The EmbeddingModel cannot be mocked, since it is used by Spring before stubbing takes effect.
 */
@TestConfiguration
class TestEmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new DummyEmbeddingModel();
    }

    @NonNullApi
    private static final class DummyEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float[] embed(Document document) {
            Random r = new Random(Objects.hashCode(document.getText()));
            return new float[]{r.nextFloat(), r.nextFloat(), r.nextFloat()};
        }
    }

}
