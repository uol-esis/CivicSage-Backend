package de.uol.pgdoener.civicsage.search;

import de.uol.pgdoener.civicsage.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.search.exception.FilterExpressionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterExpressionValidator {

    private static final List<String> validMetadataKeys = Arrays.stream(MetadataKeys.values())
            .filter(MetadataKeys::isExposed)
            .map(MetadataKeys::getValue)
            .toList();

    /**
     * This method validates that the provided filter expression only used allowed metadata keys. If an internal
     * metadata key is used, a {@link FilterExpressionException} is thrown. Keys for additional properties begin with
     * the key for additional properties ({@link MetadataKeys#ADDITIONAL_PROPERTIES}) followed by a dot.
     *
     * @param filterString the expression to validate
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

    private boolean isValid(Filter.Expression expression) {
        return switch (expression.type()) {
            case AND, OR -> isValid(expression.left()) && isValid(expression.right());
            case EQ, GTE, GT, LT, IN, NIN, LTE, NE -> isValidMetadataKey(((Filter.Key) expression.left()).key());
            case NOT -> isValid(expression.left());
        };
    }

    private boolean isValid(Filter.Operand operand) {
        if (operand instanceof Filter.Expression expression)
            return isValid(expression);
        if (operand instanceof Filter.Group(Filter.Expression content))
            return isValid(content);
        throw new IllegalStateException("Unsupported operand"); // this should not happen, if the spring implementation does not change
    }

    private boolean isValidMetadataKey(String key) {
        return validMetadataKeys.contains(key) || key.startsWith(MetadataKeys.ADDITIONAL_PROPERTIES.getValue() + ".");
    }

}
