package de.uol.pgdoener.civicsage.index.document.readers;

import de.uol.pgdoener.civicsage.index.exception.ReadUrlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.TITLE;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.URL;

@Slf4j
@RequiredArgsConstructor
public class WebsiteDocumentReader implements DocumentReader {

    private final String url;

    @Override
    public List<Document> get() {
        try {
            org.jsoup.nodes.Document jsoupDocument = Jsoup.connect(url).get();

            String title = jsoupDocument.title();
            String content = jsoupDocument.body().text();

            return List.of(Document.builder()
                    .text(content)
                    .metadata(Map.of(
                            URL, url,
                            TITLE, title
                    ))
                    .build());
        } catch (IOException e) {
            log.info("Error reading url {}", url, e);
            throw new ReadUrlException("Could not read url", e);
        }
    }

}
