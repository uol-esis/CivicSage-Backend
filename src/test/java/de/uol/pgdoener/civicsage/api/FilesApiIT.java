package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.GetObjectResponse;
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

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class FilesApiIT {

    static final String API_BASE_PATH = "/api/v1/files";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = MariaDBContainerFactory.create();
    @MockitoBean
    MinioClient minioClient;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AIProperties aiProperties;
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

    // Normal file upload

    @Test
    void testUploadFileNormal() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
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

    // Normal file download

    @Test
    void testDownloadFileMissingId() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void testDownloadFileInvalidId() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("id", "not-a-uuid")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void testDownloadFileNotFound() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("id", UUID.randomUUID().toString())
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void testDownloadFile() throws Exception {
        when(minioClient.getObject(any())).thenReturn(
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 200".getBytes()))
        );

        MvcResult uploadResult = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 200".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        MvcResult downloadResult = mockMvc.perform(get(API_BASE_PATH)
                        .param("id", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("file content 200", downloadResult.getResponse().getContentAsString());
        assertEquals("attachment; filename=\"test.txt\"", downloadResult.getResponse().getHeader("Content-Disposition"));
    }

    // Temporary file upload

    @Test
    void testUploadFileTemporary() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 300".getBytes()))
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
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 301".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 301".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(minioClient, times(1)).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertFalse(fileSource.get().isTemporary());
    }

    @Test
    void testUploadFileTemporaryTwice() throws Exception {
        MvcResult result1 = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 302".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id1 = JsonPath.read(result1.getResponse().getContentAsString(), "$.id");

        MvcResult result2 = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 302".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id2 = JsonPath.read(result2.getResponse().getContentAsString(), "$.id");

        assertEquals(id1, id2);
        verify(minioClient, times(1)).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id1));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertTrue(fileSource.get().isTemporary());
    }

    @Test
    void testUploadFilePermanentThenTemporary() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 303".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", UUID.class).isNotEmpty())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 303".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(minioClient, times(1)).putObject(any());
        Optional<FileSource> fileSource = fileSourceRepository.findById(UUID.fromString(id));
        assertTrue(fileSource.isPresent());
        assertEquals("test.txt", fileSource.get().getFileName());
        assertFalse(fileSource.get().isTemporary());
    }

    // Temporary file download

    @Test
    void testDownloadFileTemporary() throws Exception {
        MvcResult uploadResult = mockMvc.perform(multipart(API_BASE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 400".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get(API_BASE_PATH)
                        .param("id", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isNotFound())
                .andReturn();
    }

}
