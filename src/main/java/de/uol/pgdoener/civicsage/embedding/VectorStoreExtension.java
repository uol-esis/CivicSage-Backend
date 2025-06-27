package de.uol.pgdoener.civicsage.embedding;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

public interface VectorStoreExtension {

    List<Document> getById(List<UUID> documentIds);

}
