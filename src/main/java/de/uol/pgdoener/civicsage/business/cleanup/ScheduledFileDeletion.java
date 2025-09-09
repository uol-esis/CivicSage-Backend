package de.uol.pgdoener.civicsage.business.cleanup;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.index.TimeFactory;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.FileSourceRepository;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledFileDeletion {

    private final AIProperties aiProperties;
    private final TimeFactory timeFactory;
    private final FileSourceRepository fileSourceRepository;
    private final SourceService sourceService;

    @Scheduled(cron = "0 0 * * * *")
    public void deleteOldFiles() {
        final Duration unusedFileLifetime = aiProperties.getChat().getUnusedFileLifetime();
        final OffsetDateTime now = timeFactory.getCurrentTime();
        final OffsetDateTime threshold = now.minus(unusedFileLifetime);
        log.info("Looking for unused files uploaded before {}", threshold);
        final Collection<FileSource> oldFiles =
                fileSourceRepository.getFileSourcesByUploadDateBeforeAndTemporaryIsTrueAndUsedByChatsIsEmpty(threshold);
        log.info("Found {} files to delete", oldFiles.size());
        oldFiles.forEach(fileSource -> sourceService.deleteSource(fileSource.getObjectStorageId()));
        log.info("Deleted {} old files", oldFiles.size());
    }

}
