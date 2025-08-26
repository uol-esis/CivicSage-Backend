package de.uol.pgdoener.civicsage.business.source;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileSourceRepository
        extends CrudRepository<FileSource, UUID> {

    boolean existsByHashAndTemporaryIsFalse(String hash);

    Optional<FileSource> getFileSourceByHashAndTemporaryIsFalse(String hash);

    List<FileSource> getFileSourceByModelsNotContainingAndTemporaryIsFalse(String modelId);

    List<FileSource> findAllByTemporaryIsFalse();

    Optional<FileSource> findByObjectStorageIdAndTemporaryIsFalse(UUID id);

    Optional<FileSource> getFileSourceByHash(String hash);

}
