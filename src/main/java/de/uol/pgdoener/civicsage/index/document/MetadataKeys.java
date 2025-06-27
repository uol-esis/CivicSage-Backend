package de.uol.pgdoener.civicsage.index.document;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetadataKeys {

    FILE_NAME("file_name"),
    FILE_ID("file_id"),
    LINE_NUMBER("line_number"),
    START_PAGE("page_number"),
    END_PAGE("end_page_number"),
    TITLE("title"),
    URL("url"),
    ADDITIONAL_PROPERTIES("additional_properties");

    private final String value;

}
