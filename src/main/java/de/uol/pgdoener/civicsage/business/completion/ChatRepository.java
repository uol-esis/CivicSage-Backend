package de.uol.pgdoener.civicsage.business.completion;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatRepository
        extends CrudRepository<Chat, UUID> {
}
