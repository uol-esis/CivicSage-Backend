package de.uol.pgdoener.civicsage.autoconfigure;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = "civicsage.s3")
public class S3Properties {

    /**
     * The URL of the S3 service.
     */
    private String url;
    /**
     * The access key for the S3 service.
     */
    private String accessKey;
    /**
     * The secret key for the S3 service.
     */
    private String secretKey;
    /**
     * The region for the S3 service.
     */
    private String region = "garage";

    @NestedConfigurationProperty
    private Bucket bucket = new Bucket();

    @Data
    public static class Bucket {
        /**
         * The name of the bucket.
         */
        private String name = "civicsage-bucket";
    }
}
