package de.uol.pgdoener.civicsage.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

}
