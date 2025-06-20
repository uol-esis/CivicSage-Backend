package de.uol.pgdoener.civicsage.source;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileSourceRepository
        extends CrudRepository<FileSource, UUID> {

}
