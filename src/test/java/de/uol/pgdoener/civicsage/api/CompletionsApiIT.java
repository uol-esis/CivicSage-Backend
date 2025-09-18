package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.business.completion.Chat;
import de.uol.pgdoener.civicsage.business.completion.ChatRepository;
import de.uol.pgdoener.civicsage.business.completion.Role;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CompletionsApiIT {

    static final String API_BASE_PATH = "/api/v1/completions/chat";
    static final String API_FILE_UPLOAD_PATH = "/api/v1/files";
    static final String API_INDEX_URL_PATH = "/api/v1/index/url";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = MariaDBContainerFactory.create();
    @MockitoBean
    MinioClient minioClient;
    @MockitoBean
    ChatModel chatModel;
    @TestBean
    EmbeddingModel embeddingModel;

    static EmbeddingModel embeddingModel() {
        return new DummyEmbeddingModel();
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ChatRepository chatRepository;
    @Autowired
    FileSourceRepository fileSourceRepository;
    @Autowired
    VectorStore vectorStore;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    // Create Chat

    @Test
    void testChatApiGetCreate() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatId", UUID.class).isNotEmpty())
                .andExpect(jsonPath("$.embeddings", hasSize(0)))
                .andExpect(jsonPath("$.systemPrompt", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages", hasSize(0)))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        Optional<Chat> chat = chatRepository.findById(UUID.fromString(id));
        assertTrue(chat.isPresent());
        assertEquals(UUID.fromString(id), chat.get().getId());
        assertEquals(0, chat.get().getDocumentIds().size());
        assertFalse(chat.get().getSystemPrompt().isEmpty());
        assertEquals(0, chat.get().getMessages().size());
    }

    // Get Chat

    @Test
    void testChatApiGetBadRequest() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .param("chatId", "not-a-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiGetEmptyUuid() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .param("chatId", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void testChatApiGetNullUuid() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .param("chatId", (String) null)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void testChatApiGetNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get(API_BASE_PATH)
                        .param("chatId", randomId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Optional<Chat> chat = chatRepository.findById(randomId);
        assertTrue(chat.isEmpty());
    }

    @Test
    void testChatApiGetFound() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(get(API_BASE_PATH)
                        .param("chatId", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatId", UUID.class).value(id))
                .andExpect(jsonPath("$.embeddings", hasSize(0)))
                .andExpect(jsonPath("$.systemPrompt", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages", hasSize(0)));

        Optional<Chat> foundChat = chatRepository.findById(uuid);
        assertTrue(foundChat.isPresent());
        assertEquals(uuid, foundChat.get().getId());
        assertEquals(0, foundChat.get().getDocumentIds().size());
        assertFalse(foundChat.get().getSystemPrompt().isEmpty());
        assertEquals(0, foundChat.get().getMessages().size());
    }

    // Patch Chat

    @Test
    void testChatApiPatchInvalidId() throws Exception {
        mockMvc.perform(patch(API_BASE_PATH)
                        .param("chatId", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "embeddings": ["11111111-1111-1111-1111-111111111111"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiPatchNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(patch(API_BASE_PATH)
                        .param("chatId", randomId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "embeddings": ["11111111-1111-1111-1111-111111111111"]
                                }
                                """))
                .andExpect(status().isNotFound());

        Optional<Chat> chat = chatRepository.findById(randomId);
        assertTrue(chat.isEmpty());
    }

    @Test
    void testChatApiPatchAddEmbeddings() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(patch(API_BASE_PATH)
                        .param("chatId", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "embeddings": ["11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222"]
                                }
                                """))
                .andExpect(status().isOk());

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());
        assertEquals(uuid, chat.get().getId());
        assertEquals(2, chat.get().getDocumentIds().size());
        assertTrue(chat.get().getDocumentIds().contains(UUID.fromString("11111111-1111-1111-1111-111111111111")));
        assertTrue(chat.get().getDocumentIds().contains(UUID.fromString("22222222-2222-2222-2222-222222222222")));
        assertFalse(chat.get().getSystemPrompt().isEmpty());
        assertEquals(0, chat.get().getMessages().size());
    }

    @Test
    void testChatApiPatchSystemPrompt() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(patch(API_BASE_PATH)
                        .param("chatId", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systemPrompt": "This is a test prompt."
                                }
                                """))
                .andExpect(status().isOk());

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());
        assertEquals(uuid, chat.get().getId());
        assertEquals(0, chat.get().getDocumentIds().size());
        assertEquals("This is a test prompt.", chat.get().getSystemPrompt());
        assertEquals(0, chat.get().getMessages().size());
    }

    @Test
    void testChatApiPatchAddMessages() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(patch(API_BASE_PATH)
                        .param("chatId", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {"role": "user", "content": "Hello, how are you?"},
                                    {"role": "assistant", "content": "I am fine, thank you!"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());
        assertEquals(uuid, chat.get().getId());
        assertEquals(0, chat.get().getDocumentIds().size());
        assertFalse(chat.get().getSystemPrompt().isEmpty());
        assertEquals(0, chat.get().getMessages().size()); // Messages cannot be changed
    }

    @Test
    void testChatApiPatchId() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        // Try to change the chatId via PATCH (should be ignored)
        assertDoesNotThrow(() -> {
            mockMvc.perform(patch(API_BASE_PATH)
                            .param("chatId", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "chatId": "33333333-3333-3333-3333-333333333333"
                                    }
                                    """))
                    .andExpect(status().isOk());
        });

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());
        assertEquals(uuid, chat.get().getId()); // ID should not have changed
        assertEquals(0, chat.get().getDocumentIds().size());
        assertFalse(chat.get().getSystemPrompt().isEmpty());
        assertEquals(0, chat.get().getMessages().size());
    }

    // Send Message

    @Test
    void testChatApiSendMessageInvalidId() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Hello, how are you?"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiSendMessageNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", randomId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Hello, how are you?"
                                }
                                """))
                .andExpect(status().isNotFound());

        Optional<Chat> chat = chatRepository.findById(randomId);
        assertTrue(chat.isEmpty());
    }

    @Test
    void testChatApiSendMessageDefault() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user",
                                  "content": "Hello, how are you?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatId", UUID.class).value(id))
                .andExpect(jsonPath("$.embeddings", hasSize(0)))
                .andExpect(jsonPath("$.systemPrompt", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Hello, how are you?"))
                .andExpect(jsonPath("$.messages[0].files", hasSize(0)))
                .andExpect(jsonPath("$.messages[0].websiteURLs", hasSize(0)))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.messages[1].content", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages[1].files", hasSize(0)))
                .andExpect(jsonPath("$.messages[1].websiteURLs", hasSize(0)));

        Optional<Chat> chat = chatRepository.findById(UUID.fromString(id));
        assertTrue(chat.isPresent());
        assertEquals(2, chat.get().getMessages().size());
        assertEquals(Role.USER, chat.get().getMessages().get(0).getRole());
        assertEquals("Hello, how are you?", chat.get().getMessages().get(0).getContent());
        assertEquals(Role.ASSISTANT, chat.get().getMessages().get(1).getRole());
        assertEquals("Chat Model Response", chat.get().getMessages().get(1).getContent());
    }

    @Test
    void testChatApiSendMessageWithFiles() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String lastMessage = prompt.getInstructions().getLast().getText();
            if (!lastMessage.endsWith("\nHello, how are you?")
                    || !lastMessage.contains("file content 100")
                    || !lastMessage.contains("file content 101")) {
                fail("Prompt does not contain the expected user message. Actual prompt: \"" + lastMessage + "\" Expected to end with: Hello, how are you? and contain file contents 100 and 101.");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null);
        });

        MvcResult uploadResult1 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId1 = JsonPath.read(uploadResult1.getResponse().getContentAsString(), "$.id");
        MvcResult uploadResult2 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test2.txt", MediaType.TEXT_PLAIN_VALUE, "file content 101".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId2 = JsonPath.read(uploadResult2.getResponse().getContentAsString(), "$.id");
        when(minioClient.getObject(any())).thenAnswer(invocation -> {
            GetObjectArgs args = invocation.getArgument(0, GetObjectArgs.class);
            if (args.object().equals(fileId1)) {
                return new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 100".getBytes()));
            } else if (args.object().equals(fileId2)) {
                return new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 101".getBytes()));
            }
            throw new IllegalArgumentException("Unknown file ID: " + args.object());
        });

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Hello, how are you?",
                                    "files": [{"fileId": "%s"}, {"fileId": "%s"}]
                                }
                                """, fileId1, fileId2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatId", UUID.class).value(chatId))
                .andExpect(jsonPath("$.embeddings", hasSize(0)))
                .andExpect(jsonPath("$.systemPrompt", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content", endsWith("Hello, how are you?")))
                .andExpect(jsonPath("$.messages[0].files", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].files..fileId", containsInAnyOrder(fileId1, fileId2)))
                .andExpect(jsonPath("$.messages[0].files..fileName", containsInAnyOrder("test.txt", "test2.txt")))
                .andExpect(jsonPath("$.messages[0].websiteURLs", hasSize(0)))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.messages[1].content", String.class).value("Chat Model Response"))
                .andExpect(jsonPath("$.messages[1].files", hasSize(0)))
                .andExpect(jsonPath("$.messages[1].websiteURLs", hasSize(0)));

        Optional<Chat> chat = chatRepository.findById(UUID.fromString(chatId));
        assertTrue(chat.isPresent());
        assertEquals(2, chat.get().getMessages().size());
        assertEquals(Role.USER, chat.get().getMessages().getFirst().getRole());
        assertEquals("Hello, how are you?", chat.get().getMessages().getFirst().getContent());
        assertEquals(0, chat.get().getMessages().getFirst().getUrls().size());
        assertEquals(2, chat.get().getMessages().getFirst().getFileIds().size());
        assertTrue(chat.get().getMessages().get(0).getFileIds().contains(UUID.fromString(fileId1)));
        assertTrue(chat.get().getMessages().get(0).getFileIds().contains(UUID.fromString(fileId2)));
        assertEquals(Role.ASSISTANT, chat.get().getMessages().get(1).getRole());
        assertEquals("Chat Model Response", chat.get().getMessages().get(1).getContent());
    }

    @Test
    void testChatApiSendMessageWithTemporaryFileSecondInteraction() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation ->
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 200".getBytes()))
        );

        MvcResult uploadResult = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 200".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "role": "user",
                                    "content": "Message 1",
                                    "files": [{"fileId": "%s"}]
                                }
                                """, fileId)))
                .andExpect(status().isOk());

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user",
                                  "content": "Message 2"
                                }
                                """))
                .andExpect(status().isOk());

        FileSource fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertEquals(1, fileSource.getUsedByChats().size());
        assertTrue(fileSource.getUsedByChats().contains(UUID.fromString(chatId)));
    }

    @Test
    void testChatApiSendMessageWithWebsiteURLs() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String lastMessage = prompt.getInstructions().getLast().getText();
            if (!lastMessage.endsWith("\nHello, how are you?")
                    || !lastMessage.toLowerCase().contains("example")) {
                fail("Prompt does not contain the expected user message. Actual prompt: \"" + lastMessage + "\" Expected to end with: Hello, how are you? and contain contents from both website URLs.");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null);
        });

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user",
                                  "content": "Hello, how are you?",
                                  "websiteURLs": [
                                    "https://example.com",
                                    "https://uol.de"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatId", UUID.class).value(chatId))
                .andExpect(jsonPath("$.embeddings", hasSize(0)))
                .andExpect(jsonPath("$.systemPrompt", String.class).isNotEmpty())
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content", endsWith("Hello, how are you?")))
                .andExpect(jsonPath("$.messages[0].files", hasSize(0)))
                .andExpect(jsonPath("$.messages[0].websiteURLs", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].websiteURLs", contains("https://example.com", "https://uol.de")))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.messages[1].content", String.class).value("Chat Model Response"))
                .andExpect(jsonPath("$.messages[1].files", hasSize(0)))
                .andExpect(jsonPath("$.messages[1].websiteURLs", hasSize(0)));

        Optional<Chat> chat = chatRepository.findById(UUID.fromString(chatId));
        assertTrue(chat.isPresent());
        assertEquals(2, chat.get().getMessages().size());
        assertEquals(Role.USER, chat.get().getMessages().getFirst().getRole());
        assertEquals("Hello, how are you?", chat.get().getMessages().getFirst().getContent());
        assertEquals(0, chat.get().getMessages().getFirst().getFileIds().size());
        assertEquals(2, chat.get().getMessages().getFirst().getUrls().size());
        assertTrue(chat.get().getMessages().get(0).getUrls().contains(new URI("https://example.com")));
        assertTrue(chat.get().getMessages().get(0).getUrls().contains(new URI("https://uol.de")));
        assertEquals(Role.ASSISTANT, chat.get().getMessages().get(1).getRole());
        assertEquals("Chat Model Response", chat.get().getMessages().get(1).getContent());
    }

    @Test
    void testChatApiSendMessageWithExistingEmbeddings() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String lastMessage = prompt.getInstructions().getLast().getText();
            if (!lastMessage.equals("Hello, how are you?")) {
                fail("Prompt does not contain the expected user message. Actual prompt: \"" + lastMessage + "\" Expected to equal: Hello, how are you?.");
            }
            String systemPrompt = prompt.getSystemMessage().getText();
            if (!systemPrompt.contains("Example Domain")) {
                fail("System prompt does not contain the expected text from the indexed document. Actual system prompt: \"" + systemPrompt + "\" If this fails, make sure that example.com still hs the expected content.");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null);
        });
        mockMvc.perform(post(API_INDEX_URL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        // Wait a bit for the indexing to complete
        Thread.sleep(500);
        // Get the document ID of the indexed document
        List<UUID> documentIds = vectorStore.similaritySearch("test").stream()
                .map(d -> UUID.fromString(d.getId()))
                .toList();

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        // Manually add some document IDs to the chat
        Chat chatBefore = chatRepository.findById(UUID.fromString(chatId)).orElseThrow();
        chatBefore = new Chat(
                chatBefore.getId(),
                documentIds,
                chatBefore.getSystemPrompt(),
                chatBefore.getMessages(),
                OffsetDateTime.now()
        );
        chatRepository.save(chatBefore);

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user",
                                  "content": "Hello, how are you?"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
        // FIXME 404 is not a good response here
    void testChatApiSendMessageWithNonExistingEmbeddings() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String lastMessage = prompt.getInstructions().getLast().getText();
            if (!lastMessage.equals("Hello, how are you?")) {
                fail("Prompt does not contain the expected user message. Actual prompt: \"" + lastMessage + "\" Expected to equal: Hello, how are you?.");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null);
        });

        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");

        // Manually add some document IDs to the chat
        Chat chatBefore = chatRepository.findById(UUID.fromString(chatId)).orElseThrow();
        chatBefore = new Chat(
                chatBefore.getId(),
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                chatBefore.getSystemPrompt(),
                chatBefore.getMessages(),
                OffsetDateTime.now()
        );
        chatRepository.save(chatBefore);

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "user",
                                  "content": "Hello, how are you?"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    // Delete Chat

    @Test
    void testChatApiDeleteNonExisting() throws Exception {
        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", UUID.randomUUID().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testChatApiDeleteInvalidId() throws Exception {
        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", "not-a-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiDeleteExisting() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat = chatRepository.findById(uuid);
        assertTrue(deletedChat.isEmpty());
    }

    @Test
    void testChatApiDeleteTwice() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.chatId");
        UUID uuid = UUID.fromString(id);

        Optional<Chat> chat = chatRepository.findById(uuid);
        assertTrue(chat.isPresent());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat = chatRepository.findById(uuid);
        assertTrue(deletedChat.isEmpty());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testChatApiDeleteNoId() throws Exception {
        mockMvc.perform(delete(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiDeleteEmptyId() throws Exception {
        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiDeleteNullId() throws Exception {
        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", (String) null)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChatApiDeleteWithPermanentFile() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 400".getBytes()))
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );

        MvcResult uploadResult = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 400".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        MvcResult chatResult = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(chatResult.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Here is a file.",
                                  "files": [{"fileId": "%s"}]
                                }
                                """, fileId)))
                .andExpect(status().isOk());

        FileSource fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertFalse(fileSource.isTemporary());
        assertFalse(fileSource.getUsedByChats().isEmpty());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", chatId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat = chatRepository.findById(UUID.fromString(chatId));
        assertTrue(deletedChat.isEmpty());
        verify(minioClient, never()).removeObject(any());
        fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertFalse(fileSource.isTemporary());
        assertTrue(fileSource.getUsedByChats().isEmpty());
    }

    @Test
    void testChatApiDeleteWithTemporaryFile() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 500".getBytes()))
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );

        MvcResult uploadResult = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 500".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        MvcResult chatResult = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(chatResult.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Here is a file.",
                                  "files": [{"fileId": "%s"}]
                                }
                                """, fileId)))
                .andExpect(status().isOk());

        FileSource fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertTrue(fileSource.isTemporary());
        assertEquals(1, fileSource.getUsedByChats().size());
        assertTrue(fileSource.getUsedByChats().contains(UUID.fromString(chatId)));

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", chatId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat = chatRepository.findById(UUID.fromString(chatId));
        assertTrue(deletedChat.isEmpty());
        verify(minioClient).removeObject(argThat(argument -> argument.object().equals(fileId)));
        Optional<FileSource> deletedFileSource = fileSourceRepository.findById(UUID.fromString(fileId));
        assertTrue(deletedFileSource.isEmpty());
    }

    @Test
    void testChatApiDeleteWithTemporaryAndPermanentFile() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );

        MvcResult uploadResult1 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "permanent.txt", MediaType.TEXT_PLAIN_VALUE, "permanent file content".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String permanentFileId = JsonPath.read(uploadResult1.getResponse().getContentAsString(), "$.id");

        MvcResult uploadResult2 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "temporary.txt", MediaType.TEXT_PLAIN_VALUE, "temporary file content".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String temporaryFileId = JsonPath.read(uploadResult2.getResponse().getContentAsString(), "$.id");
        when(minioClient.getObject(any())).thenAnswer(invocation -> {
            GetObjectArgs args = invocation.getArgument(0, GetObjectArgs.class);
            if (args.object().equals(permanentFileId)) {
                return new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("permanent file content".getBytes()));
            } else if (args.object().equals(temporaryFileId)) {
                return new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("temporary file content".getBytes()));
            }
            throw new IllegalArgumentException("Unknown file ID: " + args.object());
        });

        MvcResult chatResult = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId = JsonPath.read(chatResult.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Here are two files.",
                                  "files": [{"fileId": "%s"}, {"fileId": "%s"}]
                                }
                                """, permanentFileId, temporaryFileId)))
                .andExpect(status().isOk());

        verify(minioClient, never()).removeObject(any());
        FileSource permanentFileSource = fileSourceRepository.findById(UUID.fromString(permanentFileId)).orElseThrow();
        assertFalse(permanentFileSource.isTemporary());
        assertEquals(1, permanentFileSource.getUsedByChats().size());
        FileSource temporaryFileSource = fileSourceRepository.findById(UUID.fromString(temporaryFileId)).orElseThrow();
        assertTrue(temporaryFileSource.isTemporary());
        assertEquals(1, temporaryFileSource.getUsedByChats().size());
        assertTrue(temporaryFileSource.getUsedByChats().contains(UUID.fromString(chatId)));

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", chatId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat = chatRepository.findById(UUID.fromString(chatId));
        assertTrue(deletedChat.isEmpty());
        verify(minioClient).removeObject(argThat(argument -> argument.object().equals(temporaryFileId)));
        verify(minioClient, never()).removeObject(argThat(argument -> argument.object().equals(permanentFileId)));
        Optional<FileSource> deletedTemporaryFileSource = fileSourceRepository.findById(UUID.fromString(temporaryFileId));
        assertTrue(deletedTemporaryFileSource.isEmpty());
        permanentFileSource = fileSourceRepository.findById(UUID.fromString(permanentFileId)).orElseThrow();
        assertFalse(permanentFileSource.isTemporary());
        assertTrue(permanentFileSource.getUsedByChats().isEmpty());
    }

    @Test
    void testChatApiSendMessageWithTemporaryFileUsedByTwoChats() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 600".getBytes()))
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Chat Model Response"))), null)
        );

        MvcResult uploadResult = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 600".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String fileId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        MvcResult chatResult1 = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId1 = JsonPath.read(chatResult1.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Here is a file.",
                                  "files": [{"fileId": "%s"}]
                                }
                                """, fileId)))
                .andExpect(status().isOk());

        MvcResult chatResult2 = mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String chatId2 = JsonPath.read(chatResult2.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(post(API_BASE_PATH)
                        .param("chatId", chatId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "role": "user",
                                  "content": "Here is the same file.",
                                  "files": [{"fileId": "%s"}]
                                }
                                """, fileId)))
                .andExpect(status().isOk());

        FileSource fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertTrue(fileSource.isTemporary());
        assertEquals(2, fileSource.getUsedByChats().size());
        assertTrue(fileSource.getUsedByChats().contains(UUID.fromString(chatId1)));
        assertTrue(fileSource.getUsedByChats().contains(UUID.fromString(chatId2)));
        verify(minioClient, never()).removeObject(any());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", chatId1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat1 = chatRepository.findById(UUID.fromString(chatId1));
        assertTrue(deletedChat1.isEmpty());
        fileSource = fileSourceRepository.findById(UUID.fromString(fileId)).orElseThrow();
        assertTrue(fileSource.isTemporary());
        assertEquals(1, fileSource.getUsedByChats().size());
        assertTrue(fileSource.getUsedByChats().contains(UUID.fromString(chatId2)));
        verify(minioClient, never()).removeObject(any());

        mockMvc.perform(delete(API_BASE_PATH)
                        .param("chatId", chatId2)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Chat> deletedChat2 = chatRepository.findById(UUID.fromString(chatId2));
        assertTrue(deletedChat2.isEmpty());
        verify(minioClient).removeObject(argThat(argument -> argument.object().equals(fileId)));
        Optional<FileSource> deletedFileSource = fileSourceRepository.findById(UUID.fromString(fileId));
        assertTrue(deletedFileSource.isEmpty());
    }


}
