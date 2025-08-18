package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final ChatFactory chatFactory;

    public ChatDto createChat() {
        Chat chat = chatFactory.createChat();
        Chat savedChat = chatRepository.save(chat);
        return chatMapper.toDto(savedChat);
    }

}
