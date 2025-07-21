package de.uol.pgdoener.civicsage.business.embedding;

import org.springframework.ai.document.Document;

import java.util.List;

public record EmbeddingTask(
        List<Document> documents
) {
}
