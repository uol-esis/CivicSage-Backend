package de.uol.pgdoener.civicsage.business.embedding.exception;

import de.uol.pgdoener.civicsage.business.embedding.VectorStoreExtension;

/**
 * This exception is thrown by {@link VectorStoreExtension}s to indicate that a
 * requested document could not be found.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

}
