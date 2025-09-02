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

    /**
     * Creates a new chat with a unique ID and empty message list.
     * The chat is saved to the repository and returned as a ChatDto.
     *
     * @return the created ChatDto
     */
    public ChatDto createChat() {
        Chat chat = chatFactory.createChat();
        Chat savedChat = chatRepository.save(chat); // savedChat now has the generated ID
        return chatMapper.toDto(savedChat);
    }

    /**
     * Retrieves a chat by its ID.
     * If the chat is found, it is converted to a ChatDto and returned.
     * If not found, a ChatNotFoundException is thrown.
     *
     * @param chatId the ID of the chat to retrieve
     * @return the ChatDto if found
     * @throws ChatNotFoundException if the chat with the given ID does not exist
     */
    public ChatDto getChat(UUID chatId) throws ChatNotFoundException {
        return chatRepository.findById(chatId)
                .map(chatMapper::toDto)
                .orElseThrow(ChatNotFoundException::new);
    }

    /**
     * Updates an existing chat's document IDs and system prompt.
     * If the chat is not found, a ChatNotFoundException is thrown.
     * The chat id cannot be changed.
     * If the new document IDs list is empty, the existing list is retained.
     * If the new system prompt is not provided, the existing prompt is retained.
     * The chat messages remain unchanged.
     * The updated chat is saved to the repository.
     *
     * @param chatId  the ID of the chat to update
     * @param chatDto the ChatDto containing the new data
     * @throws ChatNotFoundException if the chat with the given ID does not exist
     */
    @Transactional
    public void updateChat(UUID chatId, ChatDto chatDto) throws ChatNotFoundException {
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

    /**
     * Sends a message in the specified chat.
     * If the chat is not found, a ChatNotFoundException is thrown.
     * It creates a ChatMessage entity from the provided ChatMessageDto and adds it to the chat's messages.
     * It then prepares the messages for the chat model, including converting file IDs and URLs to Media objects.
     * The chat model is called with the system prompt, document IDs, media metadata, and messages from the chat object.
     * If the model call fails due to rate limiting, a ChatRateLimitException is thrown.
     * The response from the model is added as an assistant message to the chat's messages.
     * Finally, the updated chat is saved to the repository and returned as a ChatDto.
     *
     * @param chatId  the ID of the chat to send the message in
     * @param message the ChatMessageDto containing the message data
     * @return the updated ChatDto after sending the message
     * @throws ChatNotFoundException if the chat with the given ID does not exist
     */
    @Transactional
    public ChatDto sendMessage(UUID chatId, ChatMessageDto message) throws ChatNotFoundException {
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

    /**
     * Calls the chat model for the provided chat with the given messages.
     * It also passes the media metadata and document IDs as context parameters to the advisors.
     * Handles rate limit problems by throwing a ChatRateLimitException.
     *
     * @param chat             the chat which contains the system prompt and document IDs
     * @param mediaMetadataMap a map of media IDs to their metadata
     * @param messages         the list of messages to send to the model
     * @return the content of the model's response
     * @throws ChatRateLimitException if the model call fails due to rate limiting
     */
    private String callModel(Chat chat, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap, List<Message> messages)
            throws ChatRateLimitException {
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

    /**
     * Creates a Spring AI Message object from a ChatMessage.
     * It processes both file IDs and URLs to create Media objects, collecting their metadata in the provided map.
     * The message type (UserMessage or AssistantMessage) is determined by the role of the ChatMessage.
     * System role messages are not supported and will throw an IllegalArgumentException.
     * It also converts files and URLs to Media objects.
     *
     * @param chat             the chat which contains the message
     * @param chatMessage      the ChatMessage to convert
     * @param mediaMetadataMap the map to store media metadata
     * @return the created Message object
     * @throws ReadFileException        if a file cannot be found or read
     * @throws ReadUrlException         if a URL is malformed or cannot be read
     * @throws IllegalArgumentException if the message role is SYSTEM
     */
    private Message createMessage(Chat chat, ChatMessage chatMessage, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap)
            throws ReadFileException, ReadUrlException, IllegalArgumentException {
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
        };
    }

    /**
     * Creates a Media object from a file ID and adds its metadata to the provided map.
     * If the file is temporary and not already associated with the chat, the chat ID is added to the file's usedByChats
     * set.
     * If the file cannot be found or read, a ReadFileException is thrown.
     *
     * @param chat             the chat which is using the file
     * @param fileId           the ID of the file to create the Media from
     * @param mediaMetadataMap the map to store media metadata
     * @return the created Media object
     * @throws ReadFileException if the file cannot be found or read
     */
    private Media createMedia(Chat chat, UUID fileId, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap)
            throws ReadFileException {
        FileSource fileSource = sourceService.getFileSourceByIdWithTemporary(fileId).orElseThrow(() -> new SourceNotFoundException("Could not find file source with ID: " + fileId));
        if (fileSource.isTemporary()) {
            Set<UUID> chatsUsingFile = fileSource.getUsedByChats();
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

    /**
     * Determines the MIME type based on the file extension of the given file name.
     *
     * @param fileName the name of the file
     * @return the corresponding MimeType
     */
    private MimeType getMimeTypeForFileName(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> Media.Format.DOC_PDF;
            case "txt" -> Media.Format.DOC_TXT;
            case "docx" -> Media.Format.DOC_DOCX;
            case "doc" -> Media.Format.DOC_DOC;
            // TODO add more formats
            default -> {
                log.warn("Unknown file extension '{}', defaulting to TXT", extension);
                yield Media.Format.DOC_TXT;
            }
        };
    }

    /**
     * Creates a Media object from a URI and adds its metadata to the provided map.
     * If the URI is malformed or cannot be read, a ReadUrlException is thrown.
     *
     * @param uri              the URI to create the Media from
     * @param mediaMetadataMap the map to store media metadata
     * @return the created Media object
     * @throws ReadUrlException if the URI is malformed or cannot be read
     */
    private Media createMedia(URI uri, Map<String, MediaConversionAdvisor.MediaMetadata> mediaMetadataMap)
            throws ReadUrlException {
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

    /**
     * Deletes a chat by its ID.
     * If the chat is not found, a ChatNotFoundException is thrown.
     * After deleting the chat, it checks all files associated with the chat's messages.
     * If a file is marked as temporary and is not used by any other chats, it is deleted from both
     * the database and object storage.
     *
     * @param chatId the ID of the chat to delete
     * @throws ChatNotFoundException if the chat with the given ID does not exist
     */
    @Transactional
    public void deleteChat(UUID chatId) throws ChatNotFoundException {
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
            }
            FileSource fileSource = optionalFileSource.get();
            Set<UUID> chatsUsingFile = fileSource.getUsedByChats();
            chatsUsingFile.remove(chatId);
            if (fileSource.isTemporary()) {
                log.debug("File with ID {} is temporary, checking if it can be deleted", fileId);
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
            } else {
                log.debug("File with ID {} is not temporary, skipping cleanup", fileId);
                sourceService.save(fileSource);
            }
        }
    }

}
