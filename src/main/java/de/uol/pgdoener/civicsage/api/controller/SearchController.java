package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.SearchApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.SearchQueryDto;
import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import de.uol.pgdoener.civicsage.business.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchController implements SearchApiDelegate {

    private final SearchService searchService;

    @Override
    public ResponseEntity<List<SearchResultDto>> searchFiles(SearchQueryDto searchQueryDto, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        log.info("Received search query: {}", searchQueryDto.getQuery());
        List<SearchResultDto> results = searchService.search(searchQueryDto, pageNumber, pageSize);
        log.info("Returning {} results", results.size());
        return ResponseEntity.ok(results);
    }

}
