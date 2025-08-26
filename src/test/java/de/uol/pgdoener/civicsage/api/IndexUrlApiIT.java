package de.uol.pgdoener.civicsage.api;

import de.uol.pgdoener.civicsage.business.source.WebsiteSource;
import de.uol.pgdoener.civicsage.business.source.WebsiteSourceRepository;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class IndexUrlApiIT {

    static final String API_BASE_PATH = "/api/v1/index/url";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = MariaDBContainerFactory.create();
    @MockitoBean
    MinioClient minioClient;
    @TestBean
    EmbeddingModel embeddingModel;

    static EmbeddingModel embeddingModel() {
        return new DummyEmbeddingModel();
    }


    @Autowired
    MockMvc mockMvc;
    @Autowired
    WebsiteSourceRepository websiteSourceRepository;
    @Value("${spring.ai.openai.embedding.options.model}")
    String modelID;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterEach
    void afterEach() {
        websiteSourceRepository.deleteAll();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testIndexApiUrl() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        WebsiteSource source = websiteSourceRepository.findByUrl("https://example.com").orElseThrow();
        assertEquals("https://example.com", source.getUrl());
        assertEquals(LocalDate.now().getYear(), source.getUploadDate().toLocalDate().getYear());
        assertEquals(LocalDate.now().getMonth(), source.getUploadDate().toLocalDate().getMonth());
        assertEquals(LocalDate.now().getDayOfMonth(), source.getUploadDate().toLocalDate().getDayOfMonth());
        assertEquals(1, source.getModels().size());
        assertEquals(modelID, source.getModels().getFirst());
        assertEquals("https://example.com", source.getMetadata().get("url"));
    }

    @Test
    void testIndexApiUrlInvalidUrl() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "invalid-url"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiUrlMissingUrl() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiUrlEmptyUrl() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": ""
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiUrlNoProtocol() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        WebsiteSource source = websiteSourceRepository.findByUrl("https://example.com").orElseThrow();
        assertEquals("https://example.com", source.getUrl());
        assertEquals("https://example.com", source.getMetadata().get("url"));
    }

    @Test
    void testIndexApiUrlTrailingSlash() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        WebsiteSource source = websiteSourceRepository.findByUrl("https://example.com").orElseThrow();
        assertEquals("https://example.com", source.getUrl());
        assertEquals("https://example.com", source.getMetadata().get("url"));
    }

    @Test
    void testIndexApiUrlCollision() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void testIndexApiUrlCollisionWithSlash() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void testIndexApiUrlUnsupportedProtocol() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "ftp://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

}
