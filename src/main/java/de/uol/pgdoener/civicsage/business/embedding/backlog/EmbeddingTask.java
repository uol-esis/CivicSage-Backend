package de.uol.pgdoener.civicsage.business.embedding.backlog;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public record EmbeddingTask(
        UUID sourceId,
        List<Document> documents,
        AtomicBoolean isProcessing,
        AtomicBoolean isCancelled,
        CountDownLatch doneLatch
) {

    public EmbeddingTask(UUID sourceId, List<Document> documents) {
        this(sourceId, documents, new AtomicBoolean(false), new AtomicBoolean(false), new CountDownLatch(1));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingTask that = (EmbeddingTask) o;
        return Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sourceId);
    }

}
