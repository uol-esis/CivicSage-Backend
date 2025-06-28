package de.uol.pgdoener.civicsage.embedding.exception;

/**
 * This exception is thrown by {@link de.uol.pgdoener.civicsage.embedding.VectorStoreExtension}s to indicate that a
 * requested document could not be found.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

}
