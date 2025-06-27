package de.uol.pgdoener.civicsage.completion;

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

    public String summarize(List<UUID> documentIds, String userPrompt, Optional<String> systemPrompt) {
        var spec = this.chatClient.prompt()
                .advisors(advisor -> advisor.param(DocumentAdvisor.DOCUMENT_IDS_CONTEXT_KEY, documentIds))
                .user(userPrompt);

        systemPrompt.ifPresent(spec::system);

        return spec.call()
                .content();
    }

}
