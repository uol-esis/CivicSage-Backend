package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.config.CachingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingBacklog embeddingBacklog;

    public void save(List<Document> documents) {
        EmbeddingTask task = new EmbeddingTask(documents);
        embeddingBacklog.add(task);
        // TODO: maybe wait for the task to be processed.
        // For now, we just add it to the backlog and let the executor handle it.
        // Thus this method returns immediately without waiting for the task to be processed.
        // This causes status code 200 to be returned immediately, but the task might not be processed yet.
        // If we block here until the task is processed, this might lead to a lot of threads waiting although their
        // request timed out already.
    }

    @Cacheable(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME
    )
    public List<Document> search(SearchRequest search) {
        log.debug("Cache miss for embedding search");
        return vectorStore.similaritySearch(search);
    }

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void clearCache() {
        log.debug("Clearing embedding cache");
    }

}
