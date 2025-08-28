package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.CompletionsApiDelegate;
import de.uol.pgdoener.civicsage.business.completion.ChatService;
import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompletionsController implements CompletionsApiDelegate {

    private final ChatService chatService;

    @Override
    public ResponseEntity<ChatDto> getChat(Optional<UUID> chatId) {
        log.debug("Received request to get chat with ID: {}", chatId);
        if (chatId.isEmpty()) {
            ChatDto chat = chatService.createChat();
            log.debug("No chat ID provided, created new chat with ID: {}", chat.getChatId());
            return ResponseEntity.status(201).body(chat);
        } else {
            log.debug("Retrieving chat with ID: {}", chatId.get());
            ChatDto chat = chatService.getChat(chatId.get());
            return ResponseEntity.ok(chat);
        }
    }

    @Override
    public ResponseEntity<Void> updateChat(UUID chatId, ChatDto chatDto) {
        log.debug("Received request to update chat with ID: {}", chatId);
        chatService.updateChat(chatId, chatDto);
        log.debug("Chat with ID: {} updated successfully", chatId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ChatDto> sendMessage(UUID chatId, ChatMessageDto chatMessageDto) {
        log.debug("Received request to send message in chat with ID: {}", chatId);
        ChatDto chat = chatService.sendMessage(chatId, chatMessageDto);
        log.debug("Message sent in chat with ID: {}", chatId);
        return ResponseEntity.ok(chat);
    }

    @Override
    public ResponseEntity<Void> deleteChat(UUID chatId) {
        log.debug("Received request to delete chat with ID: {}", chatId);
        chatService.deleteChat(chatId);
        log.debug("Deleted chat with ID: {}", chatId);
        return ResponseEntity.noContent().build();
    }
}
