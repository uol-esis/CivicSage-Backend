package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingBacklog;
import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingPriority;
import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingTask;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingBacklog embeddingBacklog;
    private final UnusedModelsVectorStores unusedModelsVectorStores;

    public void save(List<Document> documents, UUID sourceId, EmbeddingPriority priority) {
        EmbeddingTask task = new EmbeddingTask(sourceId, documents);
        embeddingBacklog.add(task, priority);
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
        Optional<EmbeddingTask> optTask = embeddingBacklog.remove(sourceId);
        if (optTask.isPresent() && optTask.get().isProcessing().get()) {
            optTask.get().isCancelled().set(true);
            try {
                log.debug("Waiting for embedding task to finish before deleting source with id: {}", sourceId);
                optTask.get().doneLatch().await();
            } catch (InterruptedException e) {
                // Interrupting the thread may cause the deletion to happen too early
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for embedding task to finish to delete source", e);
            }
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        // The UUID has to be passed as a string. Otherwise, the filter will not work, because the UUID will not be quoted in the SQL query.
        FilterExpressionBuilder.Op op = b.eq(MetadataKeys.SOURCE_ID.getValue(), sourceId.toString());

        vectorStore.delete(op.build());
        unusedModelsVectorStores.delete(op.build());
    }

    @Cacheable(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME
    )
    public Collection<UUID> getPendingSourceIds() {
        return embeddingBacklog.getSourceIds();
    }

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void clearCache() {
        log.debug("Clearing embedding cache");
    }

}
