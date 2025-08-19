package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.CompletionsApiDelegate;
import de.uol.pgdoener.civicsage.business.completion.ChatService;
import de.uol.pgdoener.civicsage.business.completion.exception.ChatNotFoundException;
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
        if (chatId.isEmpty()) {
            ChatDto chat = chatService.createChat();
            return ResponseEntity.status(201).body(chat);
        } else {
            return chatService.getChat(chatId.get())
                    .map(ResponseEntity::ok)
                    .orElseThrow(ChatNotFoundException::new);
        }
    }

    @Override
    public ResponseEntity<Void> updateChat(UUID chatId, ChatDto chatDto) {
        chatService.updateChat(chatId, chatDto);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ChatDto> sendMessage(UUID chatId, ChatMessageDto chatMessageDto) {
        ChatDto chat = chatService.sendMessage(chatId, chatMessageDto);
        return ResponseEntity.ok(chat);
    }

    @Override
    public ResponseEntity<Void> deleteChat(UUID chatId) {
        return CompletionsApiDelegate.super.deleteChat(chatId);
    }
}
