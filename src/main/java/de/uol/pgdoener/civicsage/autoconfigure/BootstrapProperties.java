package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Data
@ConfigurationProperties(prefix = "civicsage.bootstrap")
public class BootstrapProperties {

    private Data data = new Data();

    @lombok.Data
    public static class Data {
        /**
         * Path to the Civicsage bootstrap files.
         */
        private Path directory;

    }

}
