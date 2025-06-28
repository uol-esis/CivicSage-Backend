package de.uol.pgdoener.civicsage.autoconfigure;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "civicsage.s3")
public class S3Properties {

    private String url;
    private String accessKey;
    private String secretKey;
    private String region;
    private BucketInfo bucket;

    record BucketInfo(String name) {
    }
}
