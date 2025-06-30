package de.uol.pgdoener.civicsage.index.document.reader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WebsiteDocumentReader implements DocumentReader {

    private static final String MAIN_SELECTOR = "main";

    private final String url;
    @Getter
    private final Map<String, Object> additionalMetadata = new HashMap<>();

    @Override
    public List<Document> get() {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .additionalMetadata(additionalMetadata)
                .selector(MAIN_SELECTOR)
                .build();
        List<Document> documents = new JsoupDocumentReader(url, config).read();

        if (areValid(documents)) {
            return documents;
        }

        // Try again without the main selector
        config = JsoupDocumentReaderConfig.builder()
                .additionalMetadata(additionalMetadata)
                .build();
        return new JsoupDocumentReader(url, config).read();
    }

    private boolean areValid(List<Document> documents) {
        return documents.stream()
                .anyMatch(d -> {
                    String text = d.getText();
                    return text != null && !text.isBlank();
                });
    }

}
