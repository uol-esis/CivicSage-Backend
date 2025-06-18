package de.uol.pgdoener.civicsage.embedding;

import de.uol.pgdoener.civicsage.config.CachingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public List<float[]> embedDocuments(List<Document> documents) {
        return documents.stream()
                .map(embeddingModel::embed)
                .toList();
    }

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void save(List<Document> documents) {
        vectorStore.add(documents);
    }

}
