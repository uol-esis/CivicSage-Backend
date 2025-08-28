package de.uol.pgdoener.civicsage.business.completion;

/**
 * Factory interface for creating Chat instances.
 * This is used in the {@link ChatService} to create new Chat objects containing default values.
 */
@FunctionalInterface
public interface ChatFactory {

    /**
     * Creates a new Chat instance.
     *
     * @return a new Chat object
     */
    Chat createChat();

}
