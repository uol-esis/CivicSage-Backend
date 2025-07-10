package de.uol.pgdoener.civicsage.search;

import de.uol.pgdoener.civicsage.business.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.business.search.FilterExpressionValidator;
import de.uol.pgdoener.civicsage.business.search.exception.FilterExpressionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilterExpressionValidatorTest {

    final FilterExpressionValidator filterExpressionValidator = new FilterExpressionValidator();

    @Test
    void testValidateAllowed() {
        assertTrue(MetadataKeys.FILE_ID.isExposed());
        assertTrue(MetadataKeys.FILE_NAME.isExposed());
        assertTrue(MetadataKeys.TITLE.isExposed());
        assertTrue(MetadataKeys.URL.isExposed());
        assertDoesNotThrow(() -> filterExpressionValidator.validate(
                "file_id == '6dddfa9d-1556-4a17-a177-1f45c35c73e1' AND file_name == 'test.txt' OR title NIN ['Test Title', 'HI'] AND NOT url == 'https://example.com/test' OR title IN ['value']"
        ));
    }

    @Test
    void testValidateAllowedWithAdditionalProperties() {
        assertDoesNotThrow(() -> filterExpressionValidator.validate(
                "additional_properties.test == 'value' AND additional_properties.test2 IN ['value1', 'value2']"
        ));
    }

    @Test
    void testValidateNotExposed() {
        assertFalse(MetadataKeys.LINE_NUMBER.isExposed(), "This test assumes that line_number is not exposed in the API. If it should be, this test needs to be updated.");
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate(
                "line_number == 'value'" // replace if MetadataKeys.LINE_NUMBER is exposed in the future
        ));
    }

    @Test
    void testValidateNonExistentMetadataKey() {
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate(
                "non_existent_key == 'value'"
        ));
    }

    @Test
    void testValidateParsingExceptions() {
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate(null));
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate(""));
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate(" "));
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate("42"));
        assertThrows(FilterExpressionException.class, () -> filterExpressionValidator.validate("file_name"));
    }

}
