package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ChatClient chatClient;
    private final AIProperties aiProperties;

    /**
     * This calls the {@link ChatClient} created in the
     * {@link de.uol.pgdoener.civicsage.config.ChatConfig#documentChatClient()} and sets the user and system prompt.
     * It also passes the documentIds for the {@link DocumentAdvisor} to expand the system prompt with its contents.
     * <p>
     * This method returns a string containing the complete response from the chat model.
     *
     * @param documentIds  the documentIds of documents to expand the prompt with
     * @param userPrompt   the prompt from the user
     * @param systemPrompt an optional system prompt to replace the default one
     * @return the response from the chat model
     */
    public String summarize(List<UUID> documentIds, String userPrompt, Optional<String> systemPrompt) {
        var spec = this.chatClient.prompt()
                .advisors(advisor -> advisor.param(DocumentAdvisor.DOCUMENT_IDS_CONTEXT_KEY, documentIds))
                .user("Frage: " + userPrompt);

        systemPrompt.ifPresentOrElse(spec::system, () ->
                spec.system(aiProperties.getChat().getDefaultSystemPrompt())
        );

        return spec.call()
                .content();
    }

}
