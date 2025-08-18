package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultChatFactory implements ChatFactory {

    private final AIProperties aiProperties;

    @Override
    public Chat createChat() {
        return new Chat(
                null,
                List.of(),
                aiProperties.getChat().getDefaultSystemPrompt(),
                List.of()
        );
    }

}
