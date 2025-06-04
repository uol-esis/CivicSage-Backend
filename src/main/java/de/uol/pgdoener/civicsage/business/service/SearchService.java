package de.uol.pgdoener.civicsage.business.service;

import de.uol.pgdoener.civicsage.business.dto.SearchQueryDto;
import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    public List<SearchResultDto> search(SearchQueryDto query) {
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
