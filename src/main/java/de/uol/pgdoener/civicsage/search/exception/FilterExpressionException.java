package de.uol.pgdoener.civicsage.search.exception;

/**
 * This exception is thrown by the {@link de.uol.pgdoener.civicsage.search.FilterExpressionValidator} if the expression
 * is invalid.
 */
public class FilterExpressionException extends RuntimeException {

    public FilterExpressionException(String message) {
        super(message);
    }

    public FilterExpressionException(String message, Exception e) {
        super(message, e);
    }

}
