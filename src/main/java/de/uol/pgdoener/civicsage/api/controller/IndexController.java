package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.IndexApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.index.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexController implements IndexApiDelegate {

    private final IndexService indexService;

    @Override
    public ResponseEntity<Void> indexFiles(MultipartFile file) {
        log.info("Indexing file {}", file.getOriginalFilename());
        indexService.indexFile(file);
        log.info("File {} indexed successfully", file.getOriginalFilename());
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
