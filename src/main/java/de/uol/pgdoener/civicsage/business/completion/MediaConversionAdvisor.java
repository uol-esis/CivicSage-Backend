package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

@Slf4j
@Builder
@RequiredArgsConstructor
public class MediaConversionAdvisor implements BaseAdvisor {

    private final DocumentReaderService documentReaderService;

    @NotNull
    @Override
    public ChatClientRequest before(@NotNull ChatClientRequest chatClientRequest, @NotNull AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();
        Map<String, Object> context = chatClientRequest.context();

        log.debug("Processing prompt with media conversion advisor");
        List<Message> newMessages = prompt.getInstructions().stream()
                .map(m -> {
                    if (m instanceof UserMessage userMessage) {
                        String text = userMessage.getText();
                        List<Media> media = userMessage.getMedia();
                        if (media.isEmpty()) {
                            log.debug("No media found in user message, returning original message");
                            return m;
                        }
                        String mediaText = media.stream()
                                .map(this::convertMediaToText)
                                .reduce("", (acc, converted) -> acc + "\n\n" + converted).trim();
                        log.debug("Converted {} media to text", media.size());
                        return userMessage.mutate()
                                .text(mediaText + "\n\n" + text)
                                .media(List.of())
                                .build();
                    } else {
                        return m;
                    }
                })
                .toList();

        prompt = prompt.mutate()
                .messages(newMessages)
                .build();
        log.debug("Updated prompt with converted media messages");
        return new ChatClientRequest(prompt, context);
    }

    private String convertMediaToText(Media media) {
        List<Document> documents;
        switch (media) {
            case FileMedia fm -> {
                FileSource fileSource = fm.getFileSource();
                documents = documentReaderService.read(fm.getResource(), fileSource.getFileName());
                log.debug("Read {} documents from FileMedia", documents.size());
            }
            case WebsiteMedia wm -> {
                documents = documentReaderService.readURL(wm.getUrl());
                log.debug("Read {} documents from WebsiteMedia", documents.size());
            }
            default -> {
                log.warn("Unsupported media type: {}", media.getData().getClass());
                return "";
            }
        }
        return documents.stream()
                .map(Document::getText)
                .reduce("", (acc, text) -> acc + "\n" + text).trim();
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
