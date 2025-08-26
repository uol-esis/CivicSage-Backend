package de.uol.pgdoener.civicsage.api;

import com.jayway.jsonpath.JsonPath;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import de.uol.pgdoener.civicsage.test.support.DummyEmbeddingModel;
import de.uol.pgdoener.civicsage.test.support.MariaDBContainerFactory;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class IndexFileApiIT {

    static final String API_FILE_UPLOAD_PATH = "/api/v1/files";
    static final String API_BASE_PATH = "/api/v1/index/file";

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
    FileSourceRepository fileSourceRepository;
    @Value("${spring.ai.openai.embedding.options.model}")
    String modelID;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterEach
    void afterEach() {
        fileSourceRepository.deleteAll();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testIndexApiFile() throws Exception {
        when(minioClient.getObject(any())).thenReturn(
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 100".getBytes()))
        );

        MvcResult result = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 100".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(post(API_BASE_PATH)
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

        FileSource source = fileSourceRepository.findById(uuid).orElseThrow();
        assertEquals(uuid, source.getObjectStorageId());
        assertEquals("test.txt", source.getFileName());
        assertEquals(1, source.getModels().size());
        assertEquals(modelID, source.getModels().getFirst());
    }

    @Test
    void testIndexApiFileNotFound() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "fileId": "00000000-0000-0000-0000-000420000000",
                                        "title": "Test File"
                                    }
                                ]
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    //FIXME
    @Disabled("Unsure what the correct behavior should be here")
    void testIndexApiFileNoId() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "title": "Test File"
                                    }
                                ]
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    //FIXME
    @Disabled("Unsure what the correct behavior should be here")
    void testIndexApiFileEmptyId() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "fileId": "",
                                        "title": "Test File"
                                    }
                                ]
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiFileInvalidUUID() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {
                                        "fileId": "invalid-uuid",
                                        "title": "Test File"
                                    }
                                ]
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiFileNoBody() throws Exception {
        mockMvc.perform(post(API_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIndexApiFileTwoAtOnce() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                // This will be called twice, but the content does not matter for the test
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 101".getBytes()))
        );

        MvcResult result1 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test1.txt", MediaType.TEXT_PLAIN_VALUE, "file content 101".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult result2 = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test2.txt", MediaType.TEXT_PLAIN_VALUE, "file content 102".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id1 = JsonPath.read(result1.getResponse().getContentAsString(), "$.id");
        String id2 = JsonPath.read(result2.getResponse().getContentAsString(), "$.id");
        UUID uuid1 = UUID.fromString(id1);
        UUID uuid2 = UUID.fromString(id2);

        mockMvc.perform(post(API_BASE_PATH)
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

        FileSource source1 = fileSourceRepository.findById(uuid1).orElseThrow();
        assertEquals(uuid1, source1.getObjectStorageId());
        assertEquals("test1.txt", source1.getFileName());
        assertEquals(1, source1.getModels().size());
        assertEquals(modelID, source1.getModels().getFirst());

        FileSource source2 = fileSourceRepository.findById(uuid2).orElseThrow();
        assertEquals(uuid2, source2.getObjectStorageId());
        assertEquals("test2.txt", source2.getFileName());
        assertEquals(1, source2.getModels().size());
        assertEquals(modelID, source2.getModels().getFirst());
    }

    @Test
    void testIndexApiFileDuplicate() throws Exception {
        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream("file content 103".getBytes()))
        );

        MvcResult result = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 103".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(post(API_BASE_PATH)
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

        mockMvc.perform(post(API_BASE_PATH)
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
                .andExpect(status().isConflict());

        FileSource source = fileSourceRepository.findById(uuid).orElseThrow();
        assertEquals(uuid, source.getObjectStorageId());
        assertEquals("test.txt", source.getFileName());
        assertEquals(1, source.getModels().size());
        assertEquals(modelID, source.getModels().getFirst());
    }

    @Test
    void testIndexApiFileTemporaryFile() throws Exception {
        MvcResult result = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "file content 104".getBytes()))
                        .param("temporary", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(post(API_BASE_PATH)
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
                .andExpect(status().isNotFound());

        FileSource source = fileSourceRepository.findById(uuid).orElseThrow();
        assertEquals(uuid, source.getObjectStorageId());
        assertEquals("test.txt", source.getFileName());
        assertEquals(0, source.getModels().size());
        assertTrue(source.isTemporary());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test.doc", "test.docx", "test.md", "test.odt", "test.pdf", "test.txt"})
    void testIndexApiFileVariousFormats(String filename) throws Exception {
        Resource resource = new ClassPathResource("/it/" + filename);

        when(minioClient.getObject(any())).thenAnswer(invocation ->
                new GetObjectResponse(null, null, null, null, resource.getInputStream())
        );
        MvcResult result = mockMvc.perform(multipart(API_FILE_UPLOAD_PATH)
                        .file(new MockMultipartFile("file", filename, "", resource.getInputStream()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID uuid = UUID.fromString(id);

        mockMvc.perform(post(API_BASE_PATH)
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
        FileSource source = fileSourceRepository.findById(uuid).orElseThrow();
        assertEquals(uuid, source.getObjectStorageId());
        assertEquals(filename, source.getFileName());
        assertEquals(1, source.getModels().size());
        assertEquals(modelID, source.getModels().getFirst());
    }

}
