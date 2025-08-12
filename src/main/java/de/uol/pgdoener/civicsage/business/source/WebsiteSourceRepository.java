package de.uol.pgdoener.civicsage.business.source;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebsiteSourceRepository
        extends CrudRepository<WebsiteSource, UUID> {

    boolean existsByUrl(String url);

    Optional<WebsiteSource> findByUrl(String url);

    List<WebsiteSource> getWebsiteSourceByModelsNotContaining(String modelId);

}
