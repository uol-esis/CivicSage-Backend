package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.IndexApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.index.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexController implements IndexApiDelegate {

    private final IndexService indexService;

    @Override
    public ResponseEntity<Void> indexFiles(List<IndexFilesRequestInnerDto> requests) {
        log.info("Received {} files to index", requests.size());
        for (IndexFilesRequestInnerDto request : requests) {
            log.info("Indexing file {}", request.getName());
            indexService.indexFile(request);
            log.info("File {} indexed successfully", request.getName());
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> indexWebsite(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        log.info("Indexing website {}", indexWebsiteRequestDto.getUrl());
        indexService.indexURL(indexWebsiteRequestDto);
        log.info("Website {} indexed successfully", indexWebsiteRequestDto.getUrl());
        return ResponseEntity.ok().build();
    }

}
