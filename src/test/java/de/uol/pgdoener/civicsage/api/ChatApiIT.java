package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.business.completion.Chat;
import de.uol.pgdoener.civicsage.business.completion.ChatRepository;
import de.uol.pgdoener.civicsage.test.support.DummyChatModel;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ChatApiIT {

    static final String API_BASE_PATH = "/api/v1/completions/chat";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = MariaDBContainerFactory.create();
    @MockitoBean
    MinioClient minioClient;
    @TestBean
    EmbeddingModel embeddingModel;
    @TestBean
    ChatModel chatModel;

    static EmbeddingModel embeddingModel() {
        return new DummyEmbeddingModel();
    }

    static ChatModel chatModel() {
        return new DummyChatModel();
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ChatRepository chatRepository;

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


}
