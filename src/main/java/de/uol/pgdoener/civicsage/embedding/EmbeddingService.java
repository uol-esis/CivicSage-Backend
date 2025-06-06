package de.uol.pgdoener.civicsage.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
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

    public void save(List<Document> documents) {
        vectorStore.add(documents);
    }

}
