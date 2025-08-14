package de.uol.pgdoener.civicsage.business.source;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Column(nullable = false)
    private OffsetDateTime uploadDate;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> models;

    // https://www.baeldung.com/hibernate-persist-json-object#the-jdbctypecode-annotation
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

}
