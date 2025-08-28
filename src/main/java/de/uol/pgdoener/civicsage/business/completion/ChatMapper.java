package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Chat and ChatDto, ChatMessage and ChatMessageDto.
 */
@Component
public class ChatMapper {

    /**
     * Converts a Chat entity to a ChatDto.
     * Messages with the SYSTEM role are filtered out.
     * The reference to the embeddings list is preserved.
     *
     * @param chat the Chat entity to convert
     * @return the corresponding ChatDto
     */
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

    /**
     * Converts a ChatMessage entity to a ChatMessageDto.
     * References to the files and URLs lists are preserved.
     *
     * @param chatMessage the ChatMessage entity to convert
     * @return the corresponding ChatMessageDto
     */
    public ChatMessageDto toDto(@NonNull ChatMessage chatMessage) {
        return new ChatMessageDto()
                .role(toDto(chatMessage.getRole()))
                .content(chatMessage.getContent())
                .files(chatMessage.getFileIds())
                .websiteURLs(chatMessage.getUrls());
    }

    /**
     * Converts a Role enum to a ChatMessageDto.RoleEnum.
     *
     * @param role the Role enum to convert
     * @return the corresponding ChatMessageDto.RoleEnum
     * @throws IllegalArgumentException if the role is SYSTEM, as there is no corresponding DTO role. System messages
     *                                  should be set as system prompt in ChatDto.
     */
    public ChatMessageDto.RoleEnum toDto(@NonNull Role role) throws IllegalArgumentException {
        return switch (role) {
            case USER -> ChatMessageDto.RoleEnum.USER;
            case ASSISTANT -> ChatMessageDto.RoleEnum.ASSISTANT;
            case SYSTEM -> throw new IllegalArgumentException("System role is not supported in DTO conversion");
        };
    }

    /**
     * Converts a ChatMessageDto to a ChatMessage entity.
     * References to the files and URLs lists are preserved.
     *
     * @param chat    the Chat entity to which the message belongs
     * @param message the ChatMessageDto to convert
     * @return the corresponding ChatMessage entity
     */
    public ChatMessage toEntity(Chat chat, ChatMessageDto message) {
        return new ChatMessage(
                null,
                chat,
                toEntity(message.getRole().orElseThrow()),
                message.getContent().orElse(""),
                message.getFiles(),
                message.getWebsiteURLs()
        );
    }

    /**
     * Converts a ChatMessageDto.RoleEnum to a Role enum.
     *
     * @param role the ChatMessageDto.RoleEnum to convert
     * @return the corresponding Role enum
     * @throws IllegalArgumentException if the role is UNKNOWN_DEFAULT_OPEN_API
     */
    public Role toEntity(ChatMessageDto.RoleEnum role) throws IllegalArgumentException {
        return switch (role) {
            case USER -> Role.USER;
            case ASSISTANT -> Role.ASSISTANT;
            case UNKNOWN_DEFAULT_OPEN_API ->
                    throw new IllegalArgumentException("Unknown role in DTO conversion: " + role);
        };
    }

}
