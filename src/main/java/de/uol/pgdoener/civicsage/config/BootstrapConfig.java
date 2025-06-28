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

    @Value("${civicsage.startup.data.directory:#{null}}")
    private String inputDirectory;

    private final BootstrapService bootstrapService;

    @PostConstruct
    public void init() {
        if (inputDirectory == null) {
            log.debug("Indexing local files disabled");
            return;
        }
        new Thread(() -> {
            log.info("Indexing local files from directory {}", inputDirectory);
            bootstrapService.indexDirectory(inputDirectory);
            log.info("Local indexing finished.");
        }, "local indexing").start();
    }

}
