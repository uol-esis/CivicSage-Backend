package de.uol.pgdoener.civicsage.search;

import de.uol.pgdoener.civicsage.business.dto.SearchQueryDto;
import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final VectorStore vectorStore;

    public List<SearchResultDto> search(SearchQueryDto query) {
        log.info("Searching for documents with query {}", query);

        // use SearchRequest instead of String to allow for more complex queries
        List<Document> documents = vectorStore.similaritySearch(query.getQuery());

        if (documents == null || documents.isEmpty()) {
            log.info("No documents found for query {}", query);
            return List.of();
        }

        log.info("Found {} documents for query {}", documents.size(), query);
        for (Document document : documents) {
            log.debug("Document: {}", document);
        }
        // TODO convert documents to SearchResultDto

        return List.of(
                new SearchResultDto()
                        .fileName("example.txt")
                        .fileRef("/path/to/example.txt")
                        .text("Lorem ipsum")
                        .score(0.8),
                new SearchResultDto()
                        .fileName("example2.txt")
                        .fileRef("/path/to/example2.txt")
                        .text("Dolor sit amet")
                        .score(0.653),
                new SearchResultDto()
                        .url("https://example.com")
                        .text("Consectetur adipiscing elit")
                        .score(0.24)
        );
    }

}
