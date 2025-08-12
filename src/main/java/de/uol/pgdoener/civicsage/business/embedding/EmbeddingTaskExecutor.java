package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingBacklog;
import de.uol.pgdoener.civicsage.business.embedding.backlog.EmbeddingTask;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingTaskExecutor {

    private static final int MAX_UNKNOWN_ERROR_COUNT = 5;

    private final EmbeddingService embeddingService;
    private final EmbeddingBacklog embeddingBacklog;
    private final VectorStore vectorStore;
    private final AIProperties aiProperties;

    private Thread taskExecutorThread;

    /**
     * This counter is used to track the number of unknown errors that occur during the processing of embedding tasks.
     * If too many unknown errors occur in succession, the task executor thread will be stopped to prevent excessive
     * resource usage.
     */
    private final AtomicInteger unknownErrorCount = new AtomicInteger(0);

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        taskExecutorThread = Thread.ofVirtual()
                .name("embedding-task-executor")
                .start(() -> {
                    log.info("Started embedding task executor thread");
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            EmbeddingTask task = embeddingBacklog.peek();
                            task.isProcessing().set(true);
                            log.info("Embedding task with {} documents started", task.documents().size());
                            processTask(task);
                            embeddingBacklog.remove(task);
                            task.doneLatch().countDown();
                            embeddingService.clearCache();
                            log.info("Successfully processed embedding task with {} documents", task.documents().size());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (NonTransientAiException e) {
                            // This exception is handled in the processTask method, so we can ignore it here
                        } catch (Throwable t) {
                            log.error("Error processing embedding task", t);
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

    private void processTask(EmbeddingTask task) throws InterruptedException {
        while (!task.isCancelled().get()) {
            try {
                vectorStore.add(task.documents());
                unknownErrorCount.set(0);
                break;
            } catch (NonTransientAiException e) {
                log.warn("Failed to process embedding task: {}", e.getMessage());
                handleException(task, e);
            }
        }
    }

    private void handleException(EmbeddingTask task, NonTransientAiException e) throws InterruptedException {
        if (e.getMessage().startsWith("HTTP 429")) {
            log.warn("Rate limit exceeded, retrying after a delay");
            Thread.sleep(aiProperties.getEmbedding().getRetryDelay());
        } else {
            log.error("Non-recoverable error occurred moving task to the end of the backlog to try again later. Verify the configuration of the embedding model and database: {}", e.getMessage(), e);
            embeddingBacklog.defer(task);
            int currentCount = unknownErrorCount.incrementAndGet();
            if (currentCount > MAX_UNKNOWN_ERROR_COUNT) {
                log.error("Too many unknown errors occurred, stopping the embedding task executor thread");
                Thread.currentThread().interrupt();
            } else if (currentCount > MAX_UNKNOWN_ERROR_COUNT / 2) {
                log.error("Too many unknown errors occurred, blocking task executor thread for a while to prevent too many network requests. It is recommended not to run this application until the issue is resolved.");
                Thread.sleep(aiProperties.getEmbedding().getRetryDelay().multipliedBy(2));
            }
            throw e;
        }
    }

}
