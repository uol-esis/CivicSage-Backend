package de.uol.pgdoener.civicsage.business.completion;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface ChatRepository
        extends CrudRepository<Chat, UUID> {

    Collection<Chat> getChatsByLastInteractionBefore(OffsetDateTime threshold);

}
