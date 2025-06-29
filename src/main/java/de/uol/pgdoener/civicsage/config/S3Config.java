package de.uol.pgdoener.civicsage.config;

import de.uol.pgdoener.civicsage.autoconfigure.S3Properties;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties s3Properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(s3Properties.getUrl())
                .credentials(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                .region(s3Properties.getRegion())
                .build();
    }
}
