package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.index.TimeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for creating default Chat instances.
 * The system prompt is fetched from AIProperties.
 * The created chat has no embeddings for context and no messages.
 */
@Component
@RequiredArgsConstructor
public class DefaultChatFactory implements ChatFactory {

    private final AIProperties aiProperties;
    private final TimeFactory timeFactory;

    @Override
    public Chat createChat() {
        return new Chat(
                null,
                List.of(),
                aiProperties.getChat().getDefaultSystemPrompt(),
                List.of(),
                timeFactory.getCurrentTime()
        );
    }

}
