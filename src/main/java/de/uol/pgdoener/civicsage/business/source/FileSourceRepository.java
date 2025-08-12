package de.uol.pgdoener.civicsage.business.source;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileSourceRepository
        extends CrudRepository<FileSource, UUID> {

    boolean existsByHash(String hash);

    Optional<FileSource> getFileSourceByHash(String hash);

    List<FileSource> getFileSourceByModelsNotContaining(String modelId);

}
