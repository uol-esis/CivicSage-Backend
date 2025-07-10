package de.uol.pgdoener.civicsage.business.embedding.embedding;

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

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void save(List<Document> documents) {
        vectorStore.add(documents);
    }

    @Cacheable(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME
    )
    public List<Document> search(SearchRequest search) {
        log.debug("Cache miss for embedding search");
        return vectorStore.similaritySearch(search);
    }

}
