package de.uol.pgdoener.civicsage.business.completion;

/**
 * This enum is used in the entity {@link ChatMessage} to specify the role of the message sender.
 */
public enum Role {

    /**
     * The user role represents the human user interacting with the chat system.
     */
    USER,
    /**
     * The assistant role represents the AI responding to the user.
     */
    ASSISTANT,

}
