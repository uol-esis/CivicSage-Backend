package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.completion.exception.ChatNotFoundException;
import de.uol.pgdoener.civicsage.business.completion.exception.ChatRateLimitException;
import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final ChatFactory chatFactory;
    private final ChatClient chatClient;
    private final StorageService storageService;
    private final SourceService sourceService;

    public ChatDto createChat() {
        Chat chat = chatFactory.createChat();
        Chat savedChat = chatRepository.save(chat);
        return chatMapper.toDto(savedChat);
    }

    public Optional<ChatDto> getChat(UUID chatId) {
        return chatRepository.findById(chatId)
                .map(chatMapper::toDto);
    }

    public void updateChat(UUID chatId, ChatDto chatDto) {
        final Chat chat = chatRepository.findById(chatId)
                .orElseThrow(ChatNotFoundException::new);

        List<UUID> newDocumentIds;
        // We do not allow the new list to be empty, as we cannot differentiate between an empty list and no list.
        if (chatDto.getEmbeddings().isEmpty()) {
            newDocumentIds = chat.getDocumentIds();
            log.debug("No new document IDs provided, keeping existing");
        } else {
            newDocumentIds = chatDto.getEmbeddings();
            log.debug("Updating chat with new document IDs: {}", newDocumentIds);
        }

        Chat newChat = new Chat(
                chat.getId(), // Keep the existing chat ID
                newDocumentIds,
                chatDto.getSystemPrompt().orElseGet(() -> {
                    log.debug("No new system prompt provided, keeping existing");
                    return chat.getSystemPrompt();
                }),
                chat.getMessages() // Keep the existing messages
        );
        chatRepository.save(newChat);
    }

    public ChatDto sendMessage(UUID chatId, ChatMessageDto message) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(ChatNotFoundException::new);

        ChatMessage chatMessage = chatMapper.toEntity(chat, message);
        chat.getMessages().add(chatMessage);
        log.debug("Adding message to chat {}", chatId);

        List<Message> messages = chat.getMessages().stream()
                .map(this::createMessage)
                .toList();
        log.debug("Sending message to chat {} with {} messages", chatId, messages.size());

        String content;
        try {
            content = chatClient.prompt()
                    .system(chat.getSystemPrompt())
                    .advisors(advisor -> advisor.param(DocumentAdvisor.DOCUMENT_IDS_CONTEXT_KEY, chat.getDocumentIds()))
                    .messages(messages)
                    .call()
                    .content();
        } catch (NonTransientAiException e) {
            if (e.getMessage().startsWith("HTTP 429")) {
                log.error("Rate limit exceeded for chat completion", e);
                throw new ChatRateLimitException();
            }
            log.error("Unknown error during chat completion", e);
            throw e;
        }

        ChatMessage responseMessage = new ChatMessage(
                null,
                chat,
                Role.ASSISTANT,
                content,
                List.of(),
                List.of()
        );
        chat.getMessages().add(responseMessage);
        log.debug("Received response from chat completion");

        Chat updatedChat = chatRepository.save(chat);
        return chatMapper.toDto(updatedChat);
    }

    private Message createMessage(ChatMessage chatMessage) {
        List<Media> mediaList = new java.util.ArrayList<>(chatMessage.getFileIds().stream()
                .map(this::createMedia)
                .toList());
        mediaList.addAll(chatMessage.getUrls().stream()
                .map(this::createMedia)
                .toList());
        return switch (chatMessage.getRole()) {
            case USER -> UserMessage.builder()
                    .text(chatMessage.getContent())
                    .media(mediaList)
                    .build();
            case ASSISTANT -> new AssistantMessage(
                    chatMessage.getContent(),
                    Map.of(),
                    List.of(),
                    mediaList
            );
            case SYSTEM -> throw new IllegalArgumentException("System role is not supported in message creation");
        };
    }

    private Media createMedia(UUID fileId) {
        return storageService.load(fileId)
                .map(is -> new FileMedia(new InputStreamResource(is), sourceService.getFileSourceById(fileId)))
                .orElseThrow(() -> new ReadFileException("Could not find file with ID: " + fileId));
    }

    private Media createMedia(URI uri) {
        return new WebsiteMedia(uri.toString());
    }

}
