package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.ReadUrlException;
import de.uol.pgdoener.civicsage.business.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final ChatFactory chatFactory;
    private final ChatClient chatClient;
    private final StorageService storageService;

    public ChatDto createChat() {
        Chat chat = chatFactory.createChat();
        Chat savedChat = chatRepository.save(chat);
        return chatMapper.toDto(savedChat);
    }

    public Optional<ChatDto> getChat(UUID chatId) {
        return chatRepository.findById(chatId)
                .map(chatMapper::toDto);
    }

    public ChatDto sendMessage(UUID chatId, ChatMessageDto message) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + chatId));

        ChatMessage chatMessage = chatMapper.toEntity(chat, message);
        chat.getMessages().add(chatMessage);

        List<Message> messages = chat.getMessages().stream()
                .map(this::createMessage)
                .toList();

        String content = chatClient.prompt()
                .system(chat.getSystemPrompt())
                // TODO add documents or something
                .messages(messages)
                .call()
                .content();

        ChatMessage responseMessage = new ChatMessage(
                null,
                chat,
                Role.ASSISTANT,
                content,
                List.of(),
                List.of()
        );
        chat.getMessages().add(responseMessage);

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
                .map(is -> Media.builder()
                        .data(new InputStreamResource(is))
                        .mimeType(MimeType.valueOf("application/pdf"))
                        .build())
                .orElseThrow(() -> new ReadFileException("Could not find file with ID: " + fileId));
    }

    private Media createMedia(URI uri) {
        try {
            return Media.builder()
                    .data(new UrlResource(uri))
                    .mimeType(MimeType.valueOf("application/octet-stream"))
                    .build();
        } catch (MalformedURLException e) {
            throw new ReadUrlException("Failed to read URL: " + uri, e);
        }
    }

}
