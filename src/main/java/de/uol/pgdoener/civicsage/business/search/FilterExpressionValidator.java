package de.uol.pgdoener.civicsage.business.search;

import de.uol.pgdoener.civicsage.business.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.business.search.exception.FilterExpressionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * This class validates filter expressions based on exposed metadata keys ({@link MetadataKeys}) and
 * whether they can be parsed ({@link FilterExpressionTextParser}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FilterExpressionValidator {

    private static final List<String> validMetadataKeys = MetadataKeys.EXPOSED_KEYS.stream()
            .map(MetadataKeys::getValue)
            .toList();

    /**
     * This method validates that the provided filter expression only used allowed metadata keys. If an internal
     * metadata key is used, a {@link FilterExpressionException} is thrown. Keys for additional properties begin with
     * the key for additional properties ({@link MetadataKeys#ADDITIONAL_PROPERTIES}) followed by a dot.
     *
     * @param filterString the expression to validate
     * @throws FilterExpressionException if the expression is not valid, i.e. it uses metadata keys that are not
     *                                   exposed, or it could not be parsed
     */
    public void validate(String filterString) {
        Filter.Expression expression;
        try {
            expression = new FilterExpressionTextParser().parse(filterString);
        } catch (Exception e) {
            log.debug("", e);
            throw new FilterExpressionException("Could not parse filter expression", e);
        }

        if (!isValid(expression)) {
            throw new FilterExpressionException("Filter expression used unsupported metadata keys. Refer to the server documentation");
        }
    }

    /**
     * This method recursively checks if the provided filter expression is valid.
     *
     * @param expression the expression to check
     * @return true if the expression is valid, false otherwise
     */
    private boolean isValid(Filter.Expression expression) {
        return switch (expression.type()) {
            case AND, OR -> isValid(expression.left()) && isValid(expression.right());
            case EQ, GTE, GT, LT, IN, NIN, LTE, NE -> isValidMetadataKey(((Filter.Key) expression.left()).key());
            case NOT -> isValid(expression.left());
        };
    }

    /**
     * This method checks if the provided operand is valid.
     *
     * @param operand the operand to check
     * @return true if the operand is valid, false otherwise
     */
    private boolean isValid(Filter.Operand operand) {
        if (operand instanceof Filter.Expression expression)
            return isValid(expression);
        if (operand instanceof Filter.Group(Filter.Expression content))
            return isValid(content);
        throw new IllegalStateException("Unsupported operand"); // this should not happen, if the spring implementation does not change
    }

    /**
     * This method checks if the provided metadata key is valid. Keys that are not exposed in the
     * {@link MetadataKeys} enum are considered invalid. Additionally, keys for additional properties
     * that start with the key for additional properties ({@link MetadataKeys#ADDITIONAL_PROPERTIES})
     * followed by a dot are also considered valid.
     *
     * @param key the metadata key to check
     * @return true if the key is valid, false otherwise
     */
    private boolean isValidMetadataKey(String key) {
        return validMetadataKeys.contains(key) || key.startsWith(MetadataKeys.ADDITIONAL_PROPERTIES.getValue() + ".");
    }

}
