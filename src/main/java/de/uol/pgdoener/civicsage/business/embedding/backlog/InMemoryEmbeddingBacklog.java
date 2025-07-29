package de.uol.pgdoener.civicsage.business.embedding.backlog;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Semaphore;

@Component
public class InMemoryEmbeddingBacklog implements EmbeddingBacklog {

    private final Map<EmbeddingPriority, Deque<EmbeddingTask>> backlog = new EnumMap<>(EmbeddingPriority.class);
    private final Semaphore mutex = new Semaphore(1, true);
    private final Semaphore entries = new Semaphore(0);

    private InMemoryEmbeddingBacklog() {
        for (EmbeddingPriority priority : EmbeddingPriority.values()) {
            backlog.put(priority, new ArrayDeque<>());
        }
    }

    @Override
    public void add(EmbeddingTask task, EmbeddingPriority priority) {
        mutex.acquireUninterruptibly();
        try {
            Deque<EmbeddingTask> queue = backlog.get(priority);
            // Adding a task should never fail.
            if (!queue.offer(task))
                throw new IllegalStateException("Failed to add task to backlog, this should always be possible.");
            entries.release();
        } finally {
            mutex.release();
        }
    }

    @Override
    public EmbeddingTask peek() throws InterruptedException {
        entries.acquire();
        mutex.acquireUninterruptibly();
        try {
            for (EmbeddingPriority priority : EmbeddingPriority.values()) {
                Deque<EmbeddingTask> queue = backlog.get(priority);
                if (!queue.isEmpty()) {
                    entries.release();
                    return queue.peekFirst();
                }
            }
            // This should never happen, as we only release the semaphore when a task is added.
            throw new IllegalStateException("No tasks available in the backlog, but semaphore was released.");
        } finally {
            mutex.release();
        }
    }

    @Override
    public void remove(EmbeddingTask task) {
        entries.acquireUninterruptibly();
        mutex.acquireUninterruptibly();
        try {
            for (EmbeddingPriority priority : EmbeddingPriority.values()) {
                Deque<EmbeddingTask> queue = backlog.get(priority);
                if (queue.remove(task)) {
                    // If the task was found and removed, we can exit early.
                    return;
                }
            }
        } finally {
            mutex.release();
        }
    }

    @Override
    public void remove(UUID sourceId) {
        mutex.acquireUninterruptibly();
        try {
            for (EmbeddingPriority priority : EmbeddingPriority.values()) {
                Deque<EmbeddingTask> queue = backlog.get(priority);
                queue.removeIf(t -> t.sourceId().equals(sourceId));
            }
        } finally {
            mutex.release();
        }
    }

    @Override
    public Collection<UUID> getSourceIds() {
        mutex.acquireUninterruptibly();
        try {
            return backlog.entrySet().stream()
                    .flatMap(d -> d.getValue().stream())
                    .map(EmbeddingTask::sourceId)
                    .toList();
        } finally {
            mutex.release();
        }
    }

}
