package de.uol.pgdoener.civicsage.bootstrap;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.index.IndexService;
import de.uol.pgdoener.civicsage.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.storage.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                try {
                    File f = entry.toFile();
                    if (f.isDirectory()) { // subdirectories are ignored
                        continue;
                    }
                    UUID fileId = fileService.storeFile(() -> new FileInputStream(f), f.getName());

                    IndexFilesRequestInnerDto request = new IndexFilesRequestInnerDto(fileId, f.getName());
                    // not sure if additional properties are the right place to mark this document to be loaded at startup.
                    // Maybe a dedicated metadata entry would be better... Or no marking necessary at all?
                    request.putAdditionalProperty(MetadataKeys.STARTUP_DOCUMENT.getValue(), true);
                    indexService.indexFile(request);
                } catch (RuntimeException e) {
                    log.warn("Failed to read file from path", e);
                }
            }
        } catch (IOException e) {
            throw new ReadFileException("Failed to read file from path", e);
        }
    }
}
