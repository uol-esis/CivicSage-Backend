package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.chunk.ChunkFileService;
import de.uol.pgdoener.civicsage.index.core.ChunkedFile;
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
    private final EmbeddingService embeddingService;

    public void indexFile(@NonNull MultipartFile file) {
        ChunkedFile chunkedFile = chunkFileService.process(file);
        chunkedFile = semanticChunkService.process(chunkedFile);

        embeddingService.embedChunks(chunkedFile);

        // TODO: save to vector database
    }

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        // Logic to index the website content
        log.error("Indexing URL: {}", indexWebsiteRequestDto.getUrl());
    }

}
