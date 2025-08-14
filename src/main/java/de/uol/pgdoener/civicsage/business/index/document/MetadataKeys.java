package de.uol.pgdoener.civicsage.business.index.document;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum MetadataKeys {

    SOURCE_ID("source_id", true),
    FILE_NAME("file_name", true),
    FILE_ID("file_id", true),
    LINE_NUMBER("line_number", false),
    START_PAGE("page_number", false),
    END_PAGE("end_page_number", false),
    TITLE("title", true),
    URL("url", true),
    ADDITIONAL_PROPERTIES("additional_properties", false), // this is marked as internal, but is available in filter expression using "additional_properties."
    STARTUP_DOCUMENT("startup_document", false),
    UPLOAD_DATE("upload_date", false);

    public static final List<MetadataKeys> EXPOSED_KEYS = Arrays.stream(MetadataKeys.values())
            .filter(MetadataKeys::isExposed)
            .toList();

    /**
     * The key used in the metadata map stored in the {@link org.springframework.ai.vectorstore.VectorStore}
     */
    private final String value;

    /**
     * This indicates whether this key is exposed via the api or is only used internally.
     * If it marked as internal, it might be changed if it's use by
     * {@link org.springframework.ai.document.DocumentReader}s has been stabilized.
     */
    private final boolean exposed;

}
