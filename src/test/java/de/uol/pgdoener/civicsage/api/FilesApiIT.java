package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    @Autowired
    FileSourceRepository fileSourceRepository;

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
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 1".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        verify(minioClient).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertFalse(fileSource.get().isTemporary());
    }

    @Test
    void testUploadFileEmpty() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        verify(minioClient).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertFalse(fileSource.get().isTemporary());
    }

    @Test
    void testUploadFileMissing() throws Exception {
        mockMvc.perform(multipart(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        verify(minioClient, never()).putObject(any());
    }

    @Test
    void testUploadFileTemporary() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 2".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        verify(minioClient).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertTrue(fileSource.get().isTemporary());
    }

    @Test
    void testUploadFileTemporaryThenPermanent() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 3".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 3".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(minioClient, times(1)).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertFalse(fileSource.get().isTemporary());
    }

}
