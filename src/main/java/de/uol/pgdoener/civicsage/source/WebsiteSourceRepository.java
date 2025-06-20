package de.uol.pgdoener.civicsage.source;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebsiteSourceRepository
        extends CrudRepository<WebsiteSource, UUID> {

    boolean existsByUrl(String url);

}
