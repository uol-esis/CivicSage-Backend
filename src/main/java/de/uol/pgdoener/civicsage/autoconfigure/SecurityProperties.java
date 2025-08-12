package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "civicsage.security")
public class SecurityProperties {

    /**
     * List of allowed origins for CORS.
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
