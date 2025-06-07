package de.uol.pgdoener.civicsage.index.document;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class MetadataKeys {

    public static final String FILE_NAME = "file_name";
    public static final String LINE_NUMBER = "line_number";
    public static final String START_PAGE = "page_number";
    public static final String END_PAGE = "end_page_number";
    public static final String TITLE = "title";

    public static final String URL = "url";

}
