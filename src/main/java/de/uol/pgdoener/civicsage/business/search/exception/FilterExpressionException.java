package de.uol.pgdoener.civicsage.business.search.exception;

import de.uol.pgdoener.civicsage.business.search.FilterExpressionValidator;

/**
 * This exception is thrown by the {@link FilterExpressionValidator} if the expression
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
