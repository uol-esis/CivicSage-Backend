package de.uol.pgdoener.civicsage.business.storage;

import de.uol.pgdoener.civicsage.business.index.TimeFactory;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.StorageException;
import de.uol.pgdoener.civicsage.business.source.FileHashingService;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.source.exception.SourceCollisionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final SourceService sourceService;
    private final FileHashingService fileHashingService;
    private final TimeFactory timeFactory;

    @Transactional
    public UUID storeFile(InputStreamSource iss, String fileName) {
        return storeFile(iss, fileName, false); // NOSONAR
    }

    @Transactional
    public UUID storeFile(InputStreamSource iss, String fileName, boolean temporary) {
        String hash;
        try {
            hash = fileHashingService.hash(iss.getInputStream());
        } catch (IOException e) {
            throw new ReadFileException("Could not read file.", e);
        }
        Optional<FileSource> fileSource = sourceService.getFileSourceByHash(hash);

        if (temporary) {
            if (fileSource.isPresent() && fileSource.get().isTemporary()) {
                log.info("Temporary file {} already exists with ID {}", fileName, fileSource.get().getObjectStorageId());
                return fileSource.get().getObjectStorageId();
            } else if (fileSource.isPresent()) {
                log.info("File {} already exists as permanent file with ID {}", fileName, fileSource.get().getObjectStorageId());
                return fileSource.get().getObjectStorageId();
            } else {
                UUID objectID = storeInStorage(iss);
                sourceService.save(new FileSource(objectID, fileName, hash, timeFactory.getCurrentTime(), List.of(), Map.of(), true, Set.of()));
                log.info("Temporary file {} uploaded successfully with ID {}", fileName, objectID);
                return objectID;
            }
        } else {
            if (fileSource.isPresent() && !fileSource.get().isTemporary()) {
                log.info("File {} already exists with ID {}", fileName, fileSource.get().getObjectStorageId());
                throw new SourceCollisionException("File is already uploaded");
            } else if (fileSource.isPresent()) {
                log.info("File {} already exists as temporary file with ID {}, updating to permanent", fileName, fileSource.get().getObjectStorageId());
                FileSource existing = fileSource.get();
                FileSource updated = new FileSource(
                        existing.getObjectStorageId(),
                        fileName, hash,
                        existing.getUploadDate(),
                        existing.getModels(),
                        existing.getMetadata(),
                        false,
                        Set.of() // We do not care about chats using permanent files. So we clear the list here.
                );
                sourceService.save(updated);
                return existing.getObjectStorageId();
            } else {
                UUID objectID = storeInStorage(iss);
                sourceService.save(new FileSource(objectID, fileName, hash, timeFactory.getCurrentTime(), List.of(), Map.of(), false, Set.of()));
                log.info("File {} uploaded successfully with ID {}", fileName, objectID);
                return objectID;
            }
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
