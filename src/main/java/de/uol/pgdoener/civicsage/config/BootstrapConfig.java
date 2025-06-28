package de.uol.pgdoener.civicsage.config;

import de.uol.pgdoener.civicsage.bootstrap.BootstrapService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    @Value("${civicsage.startup.data.directory}")
    private String inputDirectory;

    private final BootstrapService bootstrapService;

    @PostConstruct
    public void init() {
        log.info("Indexing files from directory {}", inputDirectory);
        bootstrapService.indexDirectory(inputDirectory);
        log.info("Indexing finished");
    }

}
