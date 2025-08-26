package de.uol.pgdoener.civicsage.api;

import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class FilesApiIT {

    static final String API_BASE_PATH = "/api/v1/files";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.8.2-ubi9")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    @MockitoBean
    MinioClient minioClient;

    @Autowired
    MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testUploadFileNormal() throws Exception {
        mockMvc.perform(multipart(API_BASE_PATH)
                        .file("file content 1", "test.txt".getBytes())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty());
    }

}
