package de.uol.pgdoener.civicsage.business.index.exception;

public class ReadUrlException extends RuntimeException {

    public ReadUrlException(String message) {
        super(message);
    }

    public ReadUrlException(String message, Throwable cause) {
        super(message, cause);
    }

}
