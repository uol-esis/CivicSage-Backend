package de.uol.pgdoener.civicsage.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${s3.url}")
    private String url;
    @Value("${s3.access.name}")
    private String accessKey;
    @Value("${s3.access.secret}")
    private String accessSecret;
    @Value("${s3.region}")
    private String region;


    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, accessSecret)
                .region(region)
                .build();
    }
}