package de.uol.pgdoener.civicsage.business.storage;

import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.StorageException;
import de.uol.pgdoener.civicsage.business.source.FileHashingService;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final SourceService sourceService;
    private final FileHashingService fileHashingService;

    public UUID storeFile(InputStreamSource iss, String fileName) {
        try {
            String hash = fileHashingService.hash(iss.getInputStream());
            sourceService.verifyFileHashNotIndexed(hash);
            UUID objectID = storeInStorage(iss);
            sourceService.save(new FileSource(objectID, fileName, hash, List.of(), Map.of()));
            log.info("File {} uploaded successfully with ID {}", fileName, objectID);
            return objectID;
        } catch (IOException e) {
            throw new ReadFileException("Could not read file.", e);
        }
    }

    public Optional<DownloadFile> loadFile(UUID id) {
        log.info("Looking for file with id {} in ObjectStorage", id);
        Optional<InputStream> optionalInputStream = storageService.load(id);
        if (optionalInputStream.isEmpty()) {
            return Optional.empty();
        }
        InputStreamResource inputStreamResource = new InputStreamResource(optionalInputStream.get());
        String fileName = sourceService.getFileSourceById(id).getFileName();

        return Optional.of(new DownloadFile(inputStreamResource, fileName));
    }

    public record DownloadFile(Resource resource, String filename) {
    }

    private UUID storeInStorage(InputStreamSource iss) {
        Optional<UUID> objectID;
        try {
            objectID = storageService.store(iss.getInputStream());
            log.info("Stored file {}", objectID);
        } catch (IOException e) {
            log.error("Error storing file", e);
            throw new ReadFileException("Could not read file", e);
        }
        if (objectID.isEmpty()) {
            throw new StorageException("Could not store file");
        }
        return objectID.get();
    }

}
