package de.uol.pgdoener.civicsage.index.exception;

public class ReadFileException extends RuntimeException {

    public ReadFileException(String message) {
        super(message);
    }

    public ReadFileException(String message, Throwable cause) {
        super(message, cause);
    }

}
