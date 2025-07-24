package de.uol.pgdoener.civicsage.business.embedding;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

public record EmbeddingTask(
        UUID sourceId,
        List<Document> documents
) {
}
