package de.uol.pgdoener.civicsage.business.completion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat in the database.
 * A chat consists of a list of messages, a system prompt, and associated document IDs.
 * The messages are stored in the order they were added.
 * The chat is identified by a unique UUID.
 * The id is generated automatically.
 */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Chat {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<UUID> documentIds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(nullable = false)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn
    private List<ChatMessage> messages;

}
