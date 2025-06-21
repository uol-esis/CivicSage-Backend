package de.uol.pgdoener.civicsage.index.document;

import lombok.Getter;

@Getter
public enum MetadataKeys {

    FILE_NAME("file_name"),
    FILE_ID("file_id"),
    LINE_NUMBER("line_number"),
    START_PAGE("page_number"),
    END_PAGE("end_page_number"),
    TITLE("title"),
    URL("url");

    private final String value;

    private MetadataKeys(String value) {
        this.value = value;
    }

}
