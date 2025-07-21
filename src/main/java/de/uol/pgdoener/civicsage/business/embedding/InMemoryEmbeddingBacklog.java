package de.uol.pgdoener.civicsage.business.embedding;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Component
public class InMemoryEmbeddingBacklog implements EmbeddingBacklog {

    private final BlockingDeque<EmbeddingTask> backlog = new LinkedBlockingDeque<>();

    @Override
    public void add(EmbeddingTask task) {
        backlog.offer(task);
    }

    @Override
    public EmbeddingTask poll() throws InterruptedException {
        return backlog.take();
    }

}
