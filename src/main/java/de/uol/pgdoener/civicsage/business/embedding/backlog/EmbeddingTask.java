package de.uol.pgdoener.civicsage.business.embedding.backlog;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EmbeddingTask(
        UUID sourceId,
        List<Document> documents
) {

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
