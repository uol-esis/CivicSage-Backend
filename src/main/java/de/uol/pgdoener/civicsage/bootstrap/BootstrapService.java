package de.uol.pgdoener.civicsage.bootstrap;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.index.IndexService;
import de.uol.pgdoener.civicsage.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.source.FileHashingService;
import de.uol.pgdoener.civicsage.source.FileSource;
import de.uol.pgdoener.civicsage.source.SourceService;
import de.uol.pgdoener.civicsage.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.storage.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BootstrapService {

    private final FileService fileService;
    private final IndexService indexService;
    private final FileHashingService fileHashingService;
    private final SourceService sourceService;

    public void indexDirectory(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                File f = entry.toFile();
                try {
                    if (f.isDirectory()) // subdirectories are ignored
                        continue;

                    // Store file in object storage or find hash in file sources if a collision happens
                    UUID fileId;
                    try {
                        fileId = fileService.storeFile(() -> new FileInputStream(f), f.getName());
                    } catch (SourceCollisionException e) {
                        log.debug("File already known, getting file source to index with potentially new model");
                        String hash = fileHashingService.hash(new FileInputStream(f));
                        Optional<FileSource> fileSource = sourceService.getFileSourceByHash(hash);
                        if (fileSource.isPresent()) {
                            log.debug("Found file id for known file");
                            fileId = fileSource.get().getObjectStorageId();
                        } else {
                            // should not happen
                            log.error("Could not find file in file sources despite of collision. This should not happen. Try again or check your database");
                            continue;
                        }
                    }

                    IndexFilesRequestInnerDto request = new IndexFilesRequestInnerDto(fileId, f.getName());
                    // not sure if additional properties are the right place to mark this document to be loaded at startup.
                    // Maybe a dedicated metadata entry would be better... Or no marking necessary at all?
                    request.putAdditionalProperty(MetadataKeys.STARTUP_DOCUMENT.getValue(), true);
                    indexService.indexFile(request);
                } catch (SourceCollisionException e) {
                    log.debug("File {} already indexed", f.getName());
                } catch (RuntimeException e) {
                    log.error("Failed to read file '{}' from local directory. Reason: {}", f.getName(), e.getMessage(), e);
                }
            }
        } catch (NoSuchFileException e) {
            log.error("Failed to open directory for local indexing. Probably the indexing directory has not been set up correctly. Check for typos in the specified path.");
        } catch (AccessDeniedException e) {
            log.error("Failed to open directory for local indexing. You might want to check if the permissions are correct. If you are using a docker container, note that the user inside the container likely differs from the user on the host.");
        } catch (IOException e) {
            log.debug("Failed to index local files: Reason: ", e);
        }
    }
}
