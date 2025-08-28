package de.uol.pgdoener.civicsage.business.source;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

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

    @Column(nullable = false)
    @TimeZoneStorage(TimeZoneStorageType.COLUMN)
    private OffsetDateTime uploadDate;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> models;

    // https://www.baeldung.com/hibernate-persist-json-object#the-jdbctypecode-annotation
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private boolean temporary;

    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<UUID> usedByChats;

}
