package de.uol.pgdoener.civicsage.completion;

import de.uol.pgdoener.civicsage.embedding.VectorStoreExtension;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@RequiredArgsConstructor
public class DocumentAdvisor implements BaseAdvisor {

    public static final String DOCUMENT_IDS_CONTEXT_KEY = "document-ids-context";

    private final VectorStoreExtension vectorStoreExtension;


    @NotNull
    @Override
    public ChatClientRequest before(@NotNull ChatClientRequest chatClientRequest, @NotNull AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();
        Map<String, Object> context = chatClientRequest.context();
        @SuppressWarnings("unchecked")
        List<UUID> documentIds = (List<UUID>) context.get(DOCUMENT_IDS_CONTEXT_KEY);

        List<Document> documents = vectorStoreExtension.getById(documentIds);

        String documentsText = documents.stream()
                .map(Document::getText)
                .reduce("", (current, d) -> current + d + "\n");

        prompt = prompt.augmentSystemMessage(systemMessage -> {
            String systemMessageText = systemMessage.getText();
            systemMessageText += documentsText;
            return systemMessage.mutate().text(systemMessageText).build();
        });

        return new ChatClientRequest(prompt, context);
    }


    @NotNull
    @Override
    public ChatClientResponse after(@NotNull ChatClientResponse chatClientResponse, @NotNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
