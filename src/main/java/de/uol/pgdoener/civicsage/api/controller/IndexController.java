package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.IndexApiDelegate;
import de.uol.pgdoener.civicsage.autoconfigure.ServerProperties;
import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.business.index.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexController implements IndexApiDelegate {

    private final IndexService indexService;
    private final ServerProperties serverProperties;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public ResponseEntity<Void> indexFiles(List<IndexFilesRequestInnerDto> requests) {
        CompletableFuture<ResponseEntity<Void>> future = CompletableFuture.supplyAsync(() -> {
            log.info("Received {} files to index", requests.size());
            for (IndexFilesRequestInnerDto request : requests) {
                log.info("Indexing file {}", request.getTitle());
                indexService.indexFile(request);
                log.info("File {} indexed successfully", request.getTitle());
            }
            return ResponseEntity.ok().build();
        }, executorService);

        return handleFuture(future);
    }

    @Override
    public ResponseEntity<Void> indexWebsite(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        CompletableFuture<ResponseEntity<Void>> future = CompletableFuture.supplyAsync(() -> {
            log.info("Indexing website {}", indexWebsiteRequestDto.getUrl());
            indexService.indexURL(indexWebsiteRequestDto);
            log.info("Website {} indexed successfully", indexWebsiteRequestDto.getUrl());
            return ResponseEntity.ok().build();
        }, executorService);

        return handleFuture(future);
    }

    private ResponseEntity<Void> handleFuture(CompletableFuture<ResponseEntity<Void>> future) {
        try {
            return future.get(serverProperties.getIndexRequestTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return ResponseEntity.accepted().build();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            else throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
