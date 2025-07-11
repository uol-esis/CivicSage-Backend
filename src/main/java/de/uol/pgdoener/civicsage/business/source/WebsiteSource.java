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
public class WebsiteSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT", unique = true)
    private String url;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> models;

}
