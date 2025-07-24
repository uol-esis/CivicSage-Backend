package de.uol.pgdoener.civicsage.business.embedding;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Component
public class InMemoryEmbeddingBacklog implements EmbeddingBacklog {

    private final BlockingDeque<EmbeddingTask> backlog = new LinkedBlockingDeque<>();

    @Override
    public void add(EmbeddingTask task) {
        // Adding a task should never fail.
        if (!backlog.offer(task))
            throw new IllegalStateException("Failed to add task to backlog, this should always be possible.");
    }

    @Override
    public EmbeddingTask peek() throws InterruptedException {
        return backlog.take();
    }

    @Override
    public void remove(EmbeddingTask task) {
        backlog.remove(task);
    }

    @Override
    public Collection<UUID> getSourceIds() {
        return backlog.stream()
                .map(EmbeddingTask::sourceId)
                .toList();
    }

}
