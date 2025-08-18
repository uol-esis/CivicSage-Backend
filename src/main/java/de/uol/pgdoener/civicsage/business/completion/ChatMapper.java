package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatDto toDto(@NonNull Chat chat) {
        return new ChatDto()
                .chatId(chat.getId())
                .messages(chat.getMessages().stream()
                        .filter(chatMessage -> chatMessage.getRole() != Role.SYSTEM)
                        .map(this::toDto)
                        .toList())
                .embeddings(chat.getDocumentIds())
                .systemPrompt(chat.getSystemPrompt());
    }

    public ChatMessageDto toDto(@NonNull ChatMessage chatMessage) {
        return new ChatMessageDto()
                .role(toDto(chatMessage.getRole()))
                .content(chatMessage.getContent())
                .files(chatMessage.getFileIds())
                .websiteURLs(chatMessage.getUrls());
    }

    public ChatMessageDto.RoleEnum toDto(@NonNull Role role) {
        return switch (role) {
            case USER -> ChatMessageDto.RoleEnum.USER;
            case ASSISTANT -> ChatMessageDto.RoleEnum.ASSISTANT;
            case SYSTEM -> throw new IllegalArgumentException("System role is not supported in DTO conversion");
        };
    }

    public ChatMessage toEntity(Chat chat, ChatMessageDto message) {
        return new ChatMessage(
                null,
                chat,
                toEntity(message.getRole().orElse(ChatMessageDto.RoleEnum.UNKNOWN_DEFAULT_OPEN_API)),
                message.getContent().orElse(""),
                message.getFiles(),
                message.getWebsiteURLs()
        );
    }

    public Role toEntity(ChatMessageDto.RoleEnum role) {
        return switch (role) {
            case USER -> Role.USER;
            case ASSISTANT -> Role.ASSISTANT;
            case UNKNOWN_DEFAULT_OPEN_API ->
                    throw new IllegalArgumentException("Unknown role in DTO conversion: " + role);
        };
    }

}
