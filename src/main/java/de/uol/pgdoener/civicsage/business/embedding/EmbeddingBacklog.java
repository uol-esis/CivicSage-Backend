package de.uol.pgdoener.civicsage.business.embedding;

import java.util.Collection;
import java.util.UUID;

public interface EmbeddingBacklog {

    /**
     * Adds a new embedding task to the backlog.
     *
     * @param task The embedding task to add.
     */
    void add(EmbeddingTask task);

    /**
     * Retrieves and removes the next embedding task from the backlog.
     * If no task is available, this method will block until a task is added.
     *
     * @return The next embedding task.
     */
    EmbeddingTask poll() throws InterruptedException;

    /**
     * Retrieves the IDs of all sources that have pending embedding tasks.
     *
     * @return A collection of UUIDs representing the source IDs with pending tasks.
     */
    Collection<UUID> getSourceIds();

}
