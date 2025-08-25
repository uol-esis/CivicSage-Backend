package de.uol.pgdoener.civicsage.api;

import de.uol.pgdoener.civicsage.api.controller.IndexController;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
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

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    EmbeddingModel embeddingModel;
    @MockitoBean
    MinioClient minioClient;

    @BeforeAll
    static void beforeAll(
            @Autowired IndexController indexController,
            @Autowired EmbeddingModel embeddingModel
    ) {
        when(embeddingModel.dimensions()).thenReturn(3);
        when(embeddingModel.embed(anyList(), any(), any())).then(invocation -> {
            List<Document> texts = invocation.getArgument(0);
            return texts.stream()
                    .map(text -> new float[]{0.1f, 0.2f, 0.3f})
                    .toList();
        });

        mariadb.start();
        IndexWebsiteRequestDto indexWebsiteRequestDto = new IndexWebsiteRequestDto()
                .url("https://www.example.com");
        indexController.indexWebsite(indexWebsiteRequestDto);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testSearchApi() throws Exception {
        when(embeddingModel.dimensions()).thenReturn(3);
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

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
                .andExpect(jsonPath("$", hasSize(1)));
    }

}
