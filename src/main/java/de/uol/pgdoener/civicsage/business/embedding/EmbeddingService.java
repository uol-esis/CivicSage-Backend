package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.business.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.config.CachingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final UnusedModelsVectorStores unusedModelsVectorStores;

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

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void delete(UUID sourceId) {
        log.info("Deleting embeddings for source with id: {}", sourceId);

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        // The UUID has to be passed as a string. Otherwise, the filter will not work, because the UUID will not be quoted in the SQL query.
        FilterExpressionBuilder.Op op = b.eq(MetadataKeys.SOURCE_ID.getValue(), sourceId.toString());

        vectorStore.delete(op.build());
        unusedModelsVectorStores.delete(op.build());
    }

}
