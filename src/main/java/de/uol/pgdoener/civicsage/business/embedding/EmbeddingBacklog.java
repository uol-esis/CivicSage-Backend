package de.uol.pgdoener.civicsage.business.embedding;

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

}
