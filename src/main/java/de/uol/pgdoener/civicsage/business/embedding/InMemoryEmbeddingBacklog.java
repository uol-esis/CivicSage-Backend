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
        // This is not strictly speaking a thread-safe operation, but only one thread uses this method.
        // If the EmbeddingTaskExecutor is no longer the only user of this method,
        // this implementation should be changed to use a more thread-safe approach.
        EmbeddingTask task = backlog.take();
        backlog.putFirst(task);
        return task;
    }

    @Override
    public void remove(EmbeddingTask task) {
        backlog.remove(task);
    }

    @Override
    public void remove(UUID sourceId) {
        backlog.removeIf(t -> t.sourceId().equals(sourceId));
    }

    @Override
    public Collection<UUID> getSourceIds() {
        return backlog.stream()
                .map(EmbeddingTask::sourceId)
                .toList();
    }

}
