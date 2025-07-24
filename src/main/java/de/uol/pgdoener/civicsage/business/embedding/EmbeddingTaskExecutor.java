package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.config.CachingConfig;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingTaskExecutor {

    private final EmbeddingBacklog embeddingBacklog;
    private final VectorStore vectorStore;

    private Thread taskExecutorThread;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        taskExecutorThread = Thread.ofVirtual()
                .name("embedding-task-executor")
                .start(() -> {
                    log.info("Started embedding task executor thread");
                    while (true) {
                        try {
                            EmbeddingTask task = embeddingBacklog.poll();
                            log.info("Embedding task with {} documents started", task.documents().size());
                            processTask(task);
                            clearCache();
                            log.info("Successfully processed embedding task with {} documents", task.documents().size());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping embedding task executor thread");
        if (taskExecutorThread != null && taskExecutorThread.isAlive()) {
            taskExecutorThread.interrupt();
            try {
                taskExecutorThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to stop embedding task executor thread gracefully", e);
            }
        }
        log.info("Embedding task executor thread stopped");
    }

    private void processTask(EmbeddingTask task) {
        int retries = 0;
        while (true) {
            try {
                vectorStore.add(task.documents());
                return;
            } catch (Exception e) {
                log.warn("Failed to process embedding task: {}", e.getMessage(), e);
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @CacheEvict(
            cacheNames = CachingConfig.SEARCH_CACHE_NAME,
            allEntries = true
    )
    public void clearCache() {
        log.debug("Clearing embedding cache");
    }

}
