package de.uol.pgdoener.civicsage.search;

import de.uol.pgdoener.civicsage.business.dto.SearchQueryDto;
import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import de.uol.pgdoener.civicsage.mapper.SearchResultMapper;
import de.uol.pgdoener.civicsage.search.exception.NotEnoughResultsAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final VectorStore vectorStore;
    private final SearchResultMapper searchResultMapper;

    public List<SearchResultDto> search(SearchQueryDto query, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        log.info("Searching for documents with query {}", query);
        int pNumber = pageNumber.orElse(DEFAULT_PAGE_NUMBER);
        int pSize = pageSize.orElse(DEFAULT_PAGE_SIZE);
        int resultsToFetch = (pNumber + 1) * pSize;
        log.debug("topK = {}", resultsToFetch);

        // use SearchRequest instead of String to allow for more complex queries
        SearchRequest searchRequest = buildSearchRequest(query, resultsToFetch);
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        if (documents == null || documents.isEmpty()) {
            log.info("No documents found for query {}", query);
            return List.of();
        }

        log.info("Found {} documents for query {}", documents.size(), query);
        documents = applyPagination(documents, pNumber, pSize);

        return searchResultMapper.toDto(documents);
    }

    private SearchRequest buildSearchRequest(SearchQueryDto query, int resultsToFetch) {
        return SearchRequest.builder()
                .query(query.getQuery())
                .topK(resultsToFetch)
                .build();
    }

    private List<Document> applyPagination(List<Document> documents, int pageNumber, int pageSize) {
        int startIndex = ((pageNumber + 1) * pageSize) - pageSize;
        log.debug("startIndex = {}", startIndex);

        if (startIndex > documents.size())
            throw new NotEnoughResultsAvailableException("Only " + documents.size() + " results were found!");

        return documents.subList(startIndex, documents.size());
    }

}
