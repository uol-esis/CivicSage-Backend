package de.uol.pgdoener.civicsage;

import de.uol.pgdoener.civicsage.business.completion.exception.ChatNotFoundException;
import de.uol.pgdoener.civicsage.business.completion.exception.ChatRateLimitException;
import de.uol.pgdoener.civicsage.business.embedding.exception.DocumentNotFoundException;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.ReadUrlException;
import de.uol.pgdoener.civicsage.business.index.exception.SplittingException;
import de.uol.pgdoener.civicsage.business.index.exception.StorageException;
import de.uol.pgdoener.civicsage.business.search.exception.FilterExpressionException;
import de.uol.pgdoener.civicsage.business.search.exception.NotEnoughResultsAvailableException;
import de.uol.pgdoener.civicsage.business.search.exception.SearchRateLimitException;
import de.uol.pgdoener.civicsage.business.source.exception.HashingException;
import de.uol.pgdoener.civicsage.business.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.business.source.exception.SourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatRateLimitException.class)
    public ResponseEntity<Object> handleChatNotFoundException(ChatRateLimitException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        log.debug("ChatRateLimitException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse.getBody());
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<Object> handleChatNotFoundException(ChatNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.NOT_FOUND, ex.getMessage());
        log.debug("ChatNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse.getBody());
    }

    @ExceptionHandler(SearchRateLimitException.class)
    public ResponseEntity<Object> handleSearchRateLimitException(SearchRateLimitException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        log.debug("SearchRateLimitException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse.getBody());
    }

    @ExceptionHandler(SplittingException.class)
    public ResponseEntity<Object> handleSplittingException(SplittingException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.I_AM_A_TEAPOT, ex.getMessage());
        log.debug("SplittingException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body(errorResponse.getBody());
    }

    @ExceptionHandler(FilterExpressionException.class)
    public ResponseEntity<Object> handleFilterExpressionException(FilterExpressionException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("FilterExpressionException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Object> handleDocumentNotFoundException(DocumentNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.NOT_FOUND, ex.getMessage());
        log.debug("DocumentNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse.getBody());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Object> handleStorageException(StorageException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        log.debug("StorageException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.getBody());
    }

    @ExceptionHandler(HashingException.class)
    public ResponseEntity<Object> handleHashingException(HashingException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        log.debug("HashingException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.getBody());
    }

    @ExceptionHandler(SourceCollisionException.class)
    public ResponseEntity<Object> handleSourceCollisionException(SourceCollisionException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.CONFLICT, ex.getMessage());
        log.debug("SourceCollisionException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse.getBody());
    }

    @ExceptionHandler(SourceNotFoundException.class)
    public ResponseEntity<Object> handleSourceNotFoundException(SourceNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.NOT_FOUND, ex.getMessage());
        log.debug("SourceNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse.getBody());
    }

    @ExceptionHandler(NotEnoughResultsAvailableException.class)
    public ResponseEntity<Object> handleNotEnoughResultsAvailableException(NotEnoughResultsAvailableException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("NotEnoughResultsAvailableException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ExceptionHandler(ReadFileException.class)
    public ResponseEntity<Object> handleReadFileException(ReadFileException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("ReadFileException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ExceptionHandler(ReadUrlException.class)
    public ResponseEntity<Object> handleReadUrlException(ReadUrlException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("ReadUrlException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.CONFLICT, ex.getMessage());
        log.debug("DataIntegrityViolationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
        log.debug("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.debug("MethodArgumentNotValidException: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

}
