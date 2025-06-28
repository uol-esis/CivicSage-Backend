package de.uol.pgdoener.civicsage.bootstrap;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.index.IndexService;
import de.uol.pgdoener.civicsage.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.storage.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BootstrapService {

    private final FileService fileService;
    private final IndexService indexService;


    public void indexDirectory(String dirPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dirPath))) {
            for (Path entry : stream) {
                File f = entry.toFile();
                try {
                    if (f.isDirectory()) // subdirectories are ignored
                        continue;

                    UUID fileId = fileService.storeFile(() -> new FileInputStream(f), f.getName());

                    IndexFilesRequestInnerDto request = new IndexFilesRequestInnerDto(fileId, f.getName());
                    // not sure if additional properties are the right place to mark this document to be loaded at startup.
                    // Maybe a dedicated metadata entry would be better... Or no marking necessary at all?
                    request.putAdditionalProperty(MetadataKeys.STARTUP_DOCUMENT.getValue(), true);
                    indexService.indexFile(request);
                } catch (RuntimeException e) {
                    log.debug("Failed to read file '{}' from local directory. Reason: {}", f.getName(), e.getMessage());
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
