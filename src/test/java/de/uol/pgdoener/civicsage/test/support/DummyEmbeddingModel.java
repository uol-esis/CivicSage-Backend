package de.uol.pgdoener.civicsage.test.support;

import io.micrometer.common.lang.NonNullApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@NonNullApi
public final class DummyEmbeddingModel implements EmbeddingModel {

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            float[] embedding = this.embed(new Document(request.getInstructions().get(i)));
            embeddings.add(new Embedding(embedding, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        Random r = new Random(Objects.hashCode(document.getText()));
        return new float[]{r.nextFloat(), r.nextFloat(), r.nextFloat()};
    }

}
