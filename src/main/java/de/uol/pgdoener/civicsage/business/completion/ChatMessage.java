package de.uol.pgdoener.civicsage.business.completion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat message in the database.
 * A chat message is associated with a chat, has a role (user or assistant), content, and optional file IDs and URLs.
 * The message is identified by a unique UUID.
 * The id is generated automatically.
 */
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Chat chat;

    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<UUID> fileIds;

    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<URI> urls;

}
