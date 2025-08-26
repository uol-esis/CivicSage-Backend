package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import de.uol.pgdoener.civicsage.business.source.WebsiteSourceRepository;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SourcesApiIT {

    static final String API_INDEX_URL_PATH = "/api/v1/index/url";
    static final String API_UPLOAD_FILE_PATH = "/api/v1/files";
    static final String API_INDEX_FILE_PATH = "/api/v1/index/file";
    static final String API_BASE_PATH = "/api/v1/sources";

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
    @Autowired
    FileSourceRepository fileSourceRepository;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterEach
    void afterEach() {
        websiteSourceRepository.deleteAll();
        fileSourceRepository.deleteAll();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testSourcesListApiEmpty() throws Exception {
        mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files", hasSize(0)))
                .andExpect(jsonPath("$.websites", hasSize(0)));
    }

    @Test
    void testSourcesListApiWithOneWebsite() throws Exception {
        mockMvc.perform(post(API_INDEX_URL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        // Wait a moment to ensure the website has been embedded
        Thread.sleep(500);

        mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files", hasSize(0)))
                .andExpect(jsonPath("$.websites", hasSize(1)))
                .andExpect(jsonPath("$.websites[0].websiteId", UUID.class).isNotEmpty())
                .andExpect(jsonPath("$.websites[0].url", is("https://example.com")))
                .andExpect(jsonPath("$.websites[0].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.websites[0].embedded", Boolean.class).value(true));
    }

    @Test
    void testSourcesListApiWithOneFile() throws Exception {
        when(minioClient.getObject(any())).thenReturn(
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 100".getBytes()))
        );

        MvcResult result = mockMvc.perform(multipart(API_UPLOAD_FILE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post(API_INDEX_FILE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                [
                                    {
                                        "fileId": "%s",
                                        "title": "Test File"
                                    }
                                ]
                                """, id))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        // Wait a moment to ensure the file has been embedded
        Thread.sleep(500);

        mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files", hasSize(1)))
                .andExpect(jsonPath("$.websites", hasSize(0)))
                .andExpect(jsonPath("$.files[0].fileId", is(id)))
                .andExpect(jsonPath("$.files[0].fileName", is("test.txt")))
                .andExpect(jsonPath("$.files[0].title", is("Test File")))
                .andExpect(jsonPath("$.files[0].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.files[0].embedded", Boolean.class).value(true));
    }

    @Test
    void testSourcesListApiWithMultipleSources() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 100".getBytes()))
        );

        mockMvc.perform(post(API_INDEX_URL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        mockMvc.perform(post(API_INDEX_URL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://uol.de"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        MvcResult result1 = mockMvc.perform(multipart(API_UPLOAD_FILE_PATH)
                        .file(new MockMultipartFile("file", "test1.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id1 = JsonPath.read(result1.getResponse().getContentAsString(), "$.id");
        MvcResult result2 = mockMvc.perform(multipart(API_UPLOAD_FILE_PATH)
                        .file(new MockMultipartFile("file", "test2.txt", MediaType.TEXT_PLAIN_VALUE, "file content 200".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id2 = JsonPath.read(result2.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post(API_INDEX_FILE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                [
                                    {
                                        "fileId": "%s",
                                        "title": "Test File 1"
                                    },
                                    {
                                        "fileId": "%s",
                                        "title": "Test File 2"
                                    }
                                ]
                                """, id1, id2))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        UUID websiteId1 = websiteSourceRepository.findByUrl("https://example.com").orElseThrow().getId();
        UUID websiteId2 = websiteSourceRepository.findByUrl("https://uol.de").orElseThrow().getId();

        // Wait a moment to ensure the sources have been embedded
        Thread.sleep(500);

        mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files", hasSize(2)))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id1 + "')]").exists())
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id1 + "')].fileName", String.class).value("test1.txt"))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id1 + "')].title", String.class).value("Test File 1"))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id1 + "')].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id1 + "')].embedded", Boolean.class).value(true))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id2 + "')]").exists())
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id2 + "')].fileName", String.class).value("test2.txt"))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id2 + "')].title", String.class).value("Test File 2"))
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id2 + "')].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.files[?(@.fileId=='" + id2 + "')].embedded", Boolean.class).value(true))
                .andExpect(jsonPath("$.websites", hasSize(2)))
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId1 + "')]").exists())
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId1 + "')].url", String.class).value("https://example.com"))
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId1 + "')].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId1 + "')].embedded", Boolean.class).value(true))
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId2 + "')]").exists())
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId2 + "')].url", String.class).value("https://uol.de"))
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId2 + "')].uploadDate", String.class).isNotEmpty())
                .andExpect(jsonPath("$.websites[?(@.websiteId=='" + websiteId2 + "')].embedded", Boolean.class).value(true));
    }

    @Test
    void testSourcesListApiTemporaryFile() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_FILE_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get(API_BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files", hasSize(0)))
                .andExpect(jsonPath("$.websites", hasSize(0)));
    }

    // TODO add tests for deleting and updating websites

}
