package de.uol.pgdoener.civicsage.business.embedding.backlog;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * An EmbeddingBacklog is a queue-like structure that holds embedding tasks.
 * Implementations of this interface must allow for concurrent access of
 * the add, remove and getSourceIds methods.
 * The peek method will only be called by a single thread, but has to be
 * thread-safe in respect to the other methods.
 * <p>
 * Furthermore, it is recommended that implementations make sure that
 * there is no task starvation.
 * Tasks should be provided in a fair order like a FIFO queue.
 */
public interface EmbeddingBacklog {

    /**
     * Adds a new embedding task to the backlog.
     *
     * @param task     The embedding task to add.
     * @param priority The priority of the embedding task.
     */
    void add(EmbeddingTask task, EmbeddingPriority priority);

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
     * If the task is not present, this method will do nothing.
     *
     * @param task The embedding task to remove.
     * @return An Optional containing the removed task if it was present, or an empty Optional if it was not.
     */
    Optional<EmbeddingTask> remove(EmbeddingTask task);

    /**
     * Removes the embedding task associated with a specific source ID from the backlog.
     * If no task is associated with the given source ID, this method will do nothing.
     *
     * @param sourceId The UUID of the source whose task should be removed.
     * @return An Optional containing the removed task if it was present, or an empty Optional if it was not.
     */
    default Optional<EmbeddingTask> remove(UUID sourceId) {
        final EmbeddingTask dummyTask = new EmbeddingTask(sourceId, Collections.emptyList());
        return remove(dummyTask);
    }

    /**
     * Defer an embedding task.
     * Implementations may choose how to handle deferring.
     * By default, this method will remove the task from the backlog
     * and add it back, giving it a low priority.
     *
     * @param task The embedding task to defer.
     */
    void defer(EmbeddingTask task);

    /**
     * Retrieves the IDs of all sources that have pending embedding tasks.
     *
     * @return A collection of UUIDs representing the source IDs with pending tasks.
     */
    Collection<UUID> getSourceIds();

}
