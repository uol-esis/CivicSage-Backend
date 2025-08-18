package de.uol.pgdoener.civicsage.business.completion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<UUID> fileIds;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<URI> urls;

}
