package de.uol.pgdoener.civicsage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
public class CivicSageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicSageApplication.class, args);
    }

}
