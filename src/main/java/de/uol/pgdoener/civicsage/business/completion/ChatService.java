package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.completion.advisors.DocumentAdvisor;
import de.uol.pgdoener.civicsage.business.completion.advisors.MediaConversionAdvisor;
import de.uol.pgdoener.civicsage.business.completion.exception.ChatNotFoundException;
import de.uol.pgdoener.civicsage.business.completion.exception.ChatRateLimitException;
import de.uol.pgdoener.civicsage.business.dto.ChatDto;
import de.uol.pgdoener.civicsage.business.dto.ChatMessageDto;
import de.uol.pgdoener.civicsage.business.index.CivicSageUrlResource;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.ReadUrlException;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.source.exception.SourceNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

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

    @Transactional
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

    @Transactional
    public ChatDto sendMessage(UUID chatId, ChatMessageDto message) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(ChatNotFoundException::new);

        ChatMessage chatMessage = chatMapper.toEntity(chat, message);
        chat.getMessages().add(chatMessage);
        log.debug("Adding message to chat {}", chatId);

        Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap = new HashMap<>();
        List<Message> messages = chat.getMessages().stream()
                .map(cm -> createMessage(chat, cm, mediaMetadataMap))
                .toList();
        log.debug("Sending message to chat {} with {} messages", chatId, messages.size());

        String content = callModel(chat, mediaMetadataMap, messages);

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

    private String callModel(Chat chat, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap, List<Message> messages) {
        String content;
        try {
            content = chatClient.prompt()
                    .system(chat.getSystemPrompt())
                    .advisors(advisor -> {
                        advisor.param(DocumentAdvisor.DOCUMENT_IDS_CONTEXT_KEY, chat.getDocumentIds());
                        advisor.param(MediaConversionAdvisor.MEDIA_METADATA_CONTEXT_KEY, mediaMetadataMap);
                    })
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
        return content;
    }

    private Message createMessage(Chat chat, ChatMessage chatMessage, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap) {
        List<Media> mediaList = new ArrayList<>(chatMessage.getFileIds().stream()
                .map(fileId -> createMedia(chat, fileId, mediaMetadataMap))
                .toList());
        mediaList.addAll(chatMessage.getUrls().stream()
                .map(uri -> createMedia(uri, mediaMetadataMap))
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

    private Media createMedia(Chat chat, UUID fileId, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap) {
        FileSource fileSource = sourceService.getFileSourceByIdWithTemporary(fileId).orElseThrow(() -> new SourceNotFoundException("Could not find file source with ID: " + fileId));
        if (fileSource.isTemporary()) {
            List<UUID> chatsUsingFile = fileSource.getUsedByChats();
            chatsUsingFile.add(chat.getId());
            fileSource = new FileSource(
                    fileSource.getObjectStorageId(),
                    fileSource.getFileName(),
                    fileSource.getHash(),
                    fileSource.getUploadDate(),
                    fileSource.getModels(),
                    fileSource.getMetadata(),
                    fileSource.isTemporary(),
                    chatsUsingFile
            );
            sourceService.save(fileSource);
        }
        String fileName = fileSource.getFileName();
        Media media;
        try {
            media = storageService.load(fileId)
                    .map(is -> Media.builder()
                            .id(UUID.randomUUID().toString())
                            .data(new InputStreamResource(is))
                            .mimeType(getMimeTypeForFileName(fileName))
                            .build())
                    .orElseThrow(() -> new ReadFileException("Could not find file with ID: " + fileId));
        } catch (IllegalArgumentException e) {
            log.error("Failed to create media for file ID {}: {}", fileId, e.getMessage());
            throw new ReadFileException("Failed to read file with ID: " + fileId, e);
        }
        mediaMetadataMap.put(media.getId(), MediaConversionAdvisor.MediaMetadata.forFile(fileName));
        return media;
    }

    private MimeType getMimeTypeForFileName(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> Media.Format.DOC_PDF;
            case "txt" -> Media.Format.DOC_TXT;
            case "doc", "docx" -> Media.Format.DOC_DOCX;
            // TODO add more formats
            default -> {
                log.warn("Unknown file extension '{}', defaulting to TXT", extension);
                yield Media.Format.DOC_TXT;
            }
        };
    }

    private Media createMedia(URI uri, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap) {
        try {
            Media media = Media.builder()
                    .id(UUID.randomUUID().toString())
                    .data(new CivicSageUrlResource(uri))
                    .mimeType(Media.Format.DOC_HTML)
                    .build();
            mediaMetadataMap.put(media.getId(), MediaConversionAdvisor.MediaMetadata.forWebsite(uri.toString()));
            return media;
        } catch (MalformedURLException e) {
            throw new ReadUrlException("Failed to read URL: " + uri, e);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create media for URL {}: {}", uri, e.getMessage());
            throw new ReadUrlException("Failed to read URL: " + uri, e);
        }
    }

    @Transactional
    public void deleteChat(UUID chatId) {
        Optional<Chat> optionalChat = chatRepository.findById(chatId);
        if (optionalChat.isEmpty()) {
            throw new ChatNotFoundException();
        }
        chatRepository.deleteById(chatId);
        List<UUID> fileIdsToCheck = optionalChat.get().getMessages().stream()
                .flatMap(m -> m.getFileIds().stream())
                .distinct()
                .toList();
        for (UUID fileId : fileIdsToCheck) {
            Optional<FileSource> optionalFileSource = sourceService.getFileSourceByIdWithTemporary(fileId);
            if (optionalFileSource.isEmpty()) {
                log.warn("File with ID {} not found while cleaning up after chat deletion", fileId);
                continue;
            } else if (!optionalFileSource.get().isTemporary()) {
                log.debug("File with ID {} is not temporary, skipping cleanup", fileId);
                continue;
            }
            FileSource fileSource = optionalFileSource.get();
            List<UUID> chatsUsingFile = fileSource.getUsedByChats();
            chatsUsingFile.remove(chatId);
            if (chatsUsingFile.isEmpty()) {
                log.debug("No more chats using file ID {}, deleting file", fileId);
                sourceService.deleteSource(fileId);
                storageService.delete(fileId);
            } else {
                FileSource updatedFileSource = new FileSource(
                        fileSource.getObjectStorageId(),
                        fileSource.getFileName(),
                        fileSource.getHash(),
                        fileSource.getUploadDate(),
                        fileSource.getModels(),
                        fileSource.getMetadata(),
                        fileSource.isTemporary(),
                        chatsUsingFile
                );
                sourceService.save(updatedFileSource);
                log.debug("File ID {} is still used by other chats, not deleting", fileId);
            }
        }
    }

}
