package de.uol.pgdoener.civicsage.business.service;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.business.infrastructure.index.ChunkFileService;
import de.uol.pgdoener.civicsage.business.infrastructure.index.SemanticChunkService;
import de.uol.pgdoener.civicsage.business.infrastructure.index.core.ChunkedFile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final ChunkFileService chunkFileService;
    private final SemanticChunkService semanticChunkService;

    public void indexFile(@NonNull MultipartFile file) {
        ChunkedFile chunkedFile = chunkFileService.process(file);
        chunkedFile = semanticChunkService.process(chunkedFile);
        // TODO: embed chunks and save to vector database
    }

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        // Logic to index the website content
        log.error("Indexing URL: {}", indexWebsiteRequestDto.getUrl());
    }

}
