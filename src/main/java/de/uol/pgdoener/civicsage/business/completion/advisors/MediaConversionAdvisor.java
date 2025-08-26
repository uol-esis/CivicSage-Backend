package de.uol.pgdoener.civicsage.business.completion.advisors;

import de.uol.pgdoener.civicsage.business.index.document.DocumentReaderService;
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
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Map;

@Slf4j
@Builder
@RequiredArgsConstructor
public class MediaConversionAdvisor implements BaseAdvisor {

    /**
     * Key for the context map to store media metadata.
     * The value must be an instance of a {@link Map} from {@link String} to {@link MediaMetadata}.
     */
    public static final String MEDIA_METADATA_CONTEXT_KEY = "media-metadata-context";

    private final DocumentReaderService documentReaderService;

    @NotNull
    @Override
    public ChatClientRequest before(@NotNull ChatClientRequest chatClientRequest, @NotNull AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();
        Map<String, Object> context = chatClientRequest.context();
        @SuppressWarnings("unchecked")
        Map<String, MediaMetadata> mediaMetadata = (Map<String, MediaMetadata>) context.get(MEDIA_METADATA_CONTEXT_KEY);

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
                                .map(med -> {
                                    MediaMetadata metadata = mediaMetadata.get(med.getId());
                                    return convertMediaToText(med, metadata);
                                })
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

    private String convertMediaToText(Media media, MediaMetadata metadata) {
        List<Document> documents;
        switch (metadata) {
            case FileMetadata(String fileName) -> {
                documents = documentReaderService.read(new ByteArrayResource(media.getDataAsByteArray()), fileName);
                log.debug("Read {} documents from FileMedia", documents.size());
            }
            case WebsiteMetadata(String url) -> {
                documents = documentReaderService.readURL(url, new ByteArrayResource(media.getDataAsByteArray()));
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

    public sealed interface MediaMetadata permits FileMetadata, WebsiteMetadata {
        static MediaMetadata forFile(String fileName) {
            return new FileMetadata(fileName);
        }

        static MediaMetadata forWebsite(String url) {
            return new WebsiteMetadata(url);
        }
    }

    private record FileMetadata(String fileName) implements MediaMetadata {
    }

    private record WebsiteMetadata(String url) implements MediaMetadata {
    }

}
