package de.uol.pgdoener.civicsage.config;

import de.uol.pgdoener.civicsage.autoconfigure.BootstrapProperties;
import de.uol.pgdoener.civicsage.bootstrap.BootstrapService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    private final BootstrapProperties bootstrapProperties;
    private final BootstrapService bootstrapService;

    @PostConstruct
    public void init() {
        if (bootstrapProperties.getData().getDirectory() == null) {
            log.debug("Indexing local files disabled");
            return;
        }
        Thread directoryIndexer = new Thread(() -> {
            log.info("Indexing local files from directory {}", bootstrapProperties.getData().getDirectory());
            bootstrapService.indexDirectory(bootstrapProperties.getData().getDirectory());
            log.info("Local indexing finished.");
        }, "local indexing");
        directoryIndexer.start();

        Thread newModelReIndexer = new Thread(() -> {
            try {
                directoryIndexer.join();
                log.info("Reindexing sources with new model");
                bootstrapService.reindexSources();
                log.info("Reindexing sources finished.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        newModelReIndexer.start();
    }

}
