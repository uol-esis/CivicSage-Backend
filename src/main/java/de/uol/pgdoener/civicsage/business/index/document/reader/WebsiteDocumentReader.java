package de.uol.pgdoener.civicsage.business.index.document.reader;

import de.uol.pgdoener.civicsage.business.index.exception.ReadUrlException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.URLConnection;
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
        try {
            Resource resource = getResource(url);
            JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                    .additionalMetadata(additionalMetadata)
                    .selector(MAIN_SELECTOR)
                    .build();
            List<Document> documents = new JsoupDocumentReader(resource, config).read();

            if (areValid(documents)) {
                return documents;
            }
        } catch (IOException e) {
            throw new ReadUrlException("Could not read website: " + url, e);
        }

        try {
            Resource resource = getResource(url);
            // Try again without the main selector
            JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                    .additionalMetadata(additionalMetadata)
                    .build();
            return new JsoupDocumentReader(resource, config).read();
        } catch (IOException e) {
            throw new ReadUrlException("Could not read website: " + url, e);
        }
    }

    private Resource getResource(String url) throws IOException {
        return new UrlResource(url) {
            @Override
            protected void customizeConnection(@NotNull URLConnection con) throws IOException {
                super.customizeConnection(con);
                con.addRequestProperty("User-Agent", "CivicSage Document Reader");
            }
        };
    }

    private boolean areValid(List<Document> documents) {
        return documents.stream()
                .allMatch(d -> {
                    String text = d.getText();
                    return text != null && !text.isBlank();
                });
    }

}
