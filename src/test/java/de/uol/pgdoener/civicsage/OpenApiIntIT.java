package de.uol.pgdoener.civicsage;

import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test checks if springdoc-openapi is correctly configured and the OpenAPI documentation is available.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntIT {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.8.2-ubi9")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    TestRestTemplate restTemplate;

    @MockitoBean
    MinioClient minioClient;

    @BeforeAll
    static void beforeAll() {
        mariadb.start();
    }

    @AfterAll
    static void afterAll() {
        mariadb.stop();
    }

    @Test
    void testSwaggerUI() {
        String url = "/v3/api-docs";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
