package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "civicsage.server")
public class ServerProperties {

    /**
     * Timeout in milliseconds for index requests.
     */
    private long indexRequestTimeout = 30_000;

}
