package de.uol.pgdoener.civicsage.api;

import de.uol.pgdoener.civicsage.api.controller.IndexController;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SearchApiIT {

    static final String API_BASE_PATH = "/api/v1/search";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.8.2-ubi9")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    @MockitoBean
    MinioClient minioClient;
    @TestBean
    EmbeddingModel embeddingModel;

    static EmbeddingModel embeddingModel() {
        return new DummyEmbeddingModel();
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeAll
    static void beforeAll(
            @Autowired IndexController indexController
    ) {
        mariadb.start();
        IndexWebsiteRequestDto indexWebsiteRequestDto = new IndexWebsiteRequestDto()
                .url("https://www.example.com");
        indexController.indexWebsite(indexWebsiteRequestDto);
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testSearchApi() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0].documentId", UUID.class).isNotEmpty())
                .andExpect(jsonPath("$.[0].title", String.class).isNotEmpty())
                .andExpect(jsonPath("$.[0].uploadDate", OffsetDateTime.class).isNotEmpty())
                .andExpect(jsonPath("$.[0].text", String.class).isNotEmpty())
                .andExpect(jsonPath("$.[0].score", Double.class).isNotEmpty())
                .andExpect(jsonPath("$.[0].url", is("https://www.example.com")))
                .andExpect(jsonPath("$.[0].fileName", String.class).isEmpty())
                .andExpect(jsonPath("$.[0].fileId", String.class).isEmpty());
    }

    @Test
    void testSearchApiMissingBody() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchApiMissingQuery() throws Exception {
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
    void testSearchApiNullQuery() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": null
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled("This has to be changed in the OpenAPI document as a `minLength = 1`. If done so, this test may be enabled.")
    void testSearchApiEmptyQuery() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": ""
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchApiNegativePageNumber() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageNumber", "-1")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchApiZeroPageNumber() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageNumber", "0")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchApiPositivePageNumber() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageNumber", "1")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // because only one result exists
    }

    @Test
    void testSearchApiNegativePageSize() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageSize", "-1")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchApiZeroPageSize() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageSize", "0")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchApiPositivePageSize() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageSize", "1")
                        .content("""
                                {
                                  "query": "Hello World"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
