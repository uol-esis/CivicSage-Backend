package de.uol.pgdoener.civicsage.business.source;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileSource {

    @Id
    private UUID objectStorageId;

    @Column(nullable = false)
    private String fileName;

    @Column(unique = true)
    private String hash;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> models;

}
