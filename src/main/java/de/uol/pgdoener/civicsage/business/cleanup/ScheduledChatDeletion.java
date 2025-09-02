package de.uol.pgdoener.civicsage.business.cleanup;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.completion.Chat;
import de.uol.pgdoener.civicsage.business.completion.ChatRepository;
import de.uol.pgdoener.civicsage.business.completion.ChatService;
import de.uol.pgdoener.civicsage.business.index.TimeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledChatDeletion {

    private final AIProperties aiProperties;
    private final TimeFactory timeFactory;
    private final ChatService chatService;
    private final ChatRepository chatRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldChats() {
        final Duration chatLifetime = aiProperties.getChat().getChatLifetime();
        final OffsetDateTime now = timeFactory.getCurrentTime();
        final OffsetDateTime threshold = now.minus(chatLifetime);
        log.info("Looking for chats to delete with last interaction before {}", threshold);
        final Collection<Chat> oldChats = chatRepository.getChatsByLastInteractionBefore(threshold);
        log.info("Found {} old chats to delete", oldChats.size());
        oldChats.forEach(chat -> chatService.deleteChat(chat.getId()));
        log.info("Deleted {} old chats", oldChats.size());
    }

}
