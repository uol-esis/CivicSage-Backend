package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.IndexApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingPriority;
import de.uol.pgdoener.civicsage.business.index.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexController implements IndexApiDelegate {

    private final IndexService indexService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public ResponseEntity<Void> indexFiles(List<IndexFilesRequestInnerDto> requests) {
        executorService.submit(() -> {
            log.info("Received {} files to index", requests.size());
            for (IndexFilesRequestInnerDto request : requests) {
                log.info("Indexing file {}", request.getTitle());
                indexService.indexFile(request, EmbeddingPriority.HIGH);
                log.info("File {} indexed successfully", request.getTitle());
            }
        });

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> indexWebsite(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        executorService.submit(() -> {
            log.info("Indexing website {}", indexWebsiteRequestDto.getUrl());
            indexService.indexURL(indexWebsiteRequestDto, EmbeddingPriority.HIGH);
            log.info("Website {} indexed successfully", indexWebsiteRequestDto.getUrl());
        });

        return ResponseEntity.accepted().build();
    }

}
