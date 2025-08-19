package de.uol.pgdoener.civicsage.business.search;

import de.uol.pgdoener.civicsage.business.dto.SearchQueryDto;
import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import de.uol.pgdoener.civicsage.business.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.business.search.exception.NotEnoughResultsAvailableException;
import de.uol.pgdoener.civicsage.business.search.exception.SearchRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    /**
     * The number of pages to fetch in one go when searching.
     */
    private static final int PAGE_CACHE_WINDOW_SIZE = 4;

    private final EmbeddingService embeddingService;
    private final SearchResultMapper searchResultMapper;
    private final FilterExpressionValidator filterExpressionValidator;

    public List<SearchResultDto> search(SearchQueryDto query, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        log.info("Searching for documents with query {}", query);
        int pNumber = pageNumber.orElse(DEFAULT_PAGE_NUMBER);
        int pSize = pageSize.orElse(DEFAULT_PAGE_SIZE);
        int resultsToFetch = calculateResultsToFetch(pNumber, pSize);
        log.debug("topK = {}", resultsToFetch);

        Optional<String> filterString = query.getFilterExpression();
        filterString.ifPresent(filterExpressionValidator::validate);

        SearchRequest searchRequest = buildSearchRequest(query.getQuery(), filterString, resultsToFetch);
        List<Document> documents;
        try {
            documents = embeddingService.search(searchRequest);
        } catch (NonTransientAiException e) {
            if (e.getMessage().startsWith("HTTP 429")) {
                log.warn("Rate limit exceeded while searching for documents: {}", e.getMessage());
                throw new SearchRateLimitException();
            }
            log.error("Error while searching for documents: {}", e.getMessage(), e);
            throw e;
        }

        if (documents == null || documents.isEmpty()) {
            log.info("No documents found for query {}", query);
            return List.of();
        }

        log.info("Found {} documents", documents.size());
        documents = applyPagination(documents, pNumber, pSize);

        return searchResultMapper.toDto(documents);
    }

    private int calculateResultsToFetch(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Page number must be >= 0 and page size must be > 0");
        }

        // Which cache window are we in?
        int cacheWindow = (pageNumber / PAGE_CACHE_WINDOW_SIZE) + 1;
        // Calculate how many pages we need to fetch
        int pagesToFetch = (cacheWindow * PAGE_CACHE_WINDOW_SIZE);
        log.debug("pagesToFetch = {}", pagesToFetch);

        return pagesToFetch * pageSize;
    }

    private SearchRequest buildSearchRequest(String query, Optional<String> filterString, int resultsToFetch) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(resultsToFetch);
        filterString.ifPresent(builder::filterExpression);
        return builder.build();
    }

    private List<Document> applyPagination(List<Document> documents, int pageNumber, int pageSize) {
        int startIndex = pageNumber * pageSize;
        int endIndex = startIndex + pageSize;
        if (endIndex > documents.size()) {
            endIndex = documents.size();
        }

        log.debug("startIndex = {}", startIndex);
        log.debug("endIndex = {}", endIndex);

        if (startIndex >= documents.size())
            throw new NotEnoughResultsAvailableException("Only " + documents.size() + " results were found!");

        return documents.subList(startIndex, endIndex);
    }

}
