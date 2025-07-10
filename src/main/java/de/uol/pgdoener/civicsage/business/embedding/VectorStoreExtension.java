package de.uol.pgdoener.civicsage.business.embedding;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

/**
 * This interface provides methods not provided by the {@link org.springframework.ai.vectorstore.VectorStore} but are
 * needed to retrieve specific documents.
 * <p>
 * Since not all {@code VectorStore}s have the same native client or behaviour, this interface has to be implemented
 * for each implementation of the {@code VectorStore} interface.
 */
public interface VectorStoreExtension {

    List<Document> getById(List<UUID> documentIds);

}
