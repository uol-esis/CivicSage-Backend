package de.uol.pgdoener.civicsage.business.embedding;

import java.util.Collection;
import java.util.UUID;

/**
 * An EmbeddingBacklog is a queue-like structure that holds embedding tasks.
 * Implementations of this interface should preserve the order of tasks
 * and allow for concurrent access.
 */
public interface EmbeddingBacklog {

    /**
     * Adds a new embedding task to the backlog.
     *
     * @param task The embedding task to add.
     */
    void add(EmbeddingTask task);

    /**
     * Retrieves the next embedding task from the backlog.
     * If no task is available, this method will block until a task is added.
     * Notably, this method does not remove the task from the backlog.
     * After calling this method, the returned task can be removed from the backlog
     * using {@link #remove(EmbeddingTask)} if it is no longer needed.
     *
     * @return The next embedding task.
     */
    EmbeddingTask peek() throws InterruptedException;

    /**
     * Removes a specific embedding task from the backlog.
     *
     * @param task The embedding task to remove.
     */
    void remove(EmbeddingTask task);

    /**
     * Defer an embedding task by moving it back to the end.
     *
     * @param task The embedding task to defer.
     */
    default void defer(EmbeddingTask task) {
        remove(task);
        add(task);
    }

    /**
     * Retrieves the IDs of all sources that have pending embedding tasks.
     *
     * @return A collection of UUIDs representing the source IDs with pending tasks.
     */
    Collection<UUID> getSourceIds();

}
