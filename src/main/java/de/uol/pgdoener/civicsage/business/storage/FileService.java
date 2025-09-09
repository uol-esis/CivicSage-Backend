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

/**
 * Logic for storing and retrieving files via the /files API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final SourceService sourceService;
    private final FileHashingService fileHashingService;
    private final TimeFactory timeFactory;

    /**
     * Stores a file as a permanent file.
     * See {@link #storeFile(InputStreamSource, String, boolean)} for details.
     */
    @Transactional
    public UUID storeFile(InputStreamSource iss, String fileName) {
        return storeFile(iss, fileName, false); // NOSONAR
    }

    /**
     * Stores a file with the given file name and temporary flag. Depending on the temporary flag and whether the file
     * already exists, different actions are taken:
     *
     * <ul>
     * <li>If temporary is true and the file already exists as a temporary file, the existing file ID is returned.</li>
     * <li>If temporary is true and the file already exists as a permanent file, the existing file ID is returned.</li>
     * <li>If temporary is true and the file does not exist, it is stored as a temporary file.</li>
     * <li>If temporary is false and the file already exists as a permanent file, a SourceCollisionException is thrown.
     * </li>
     * <li>If temporary is false and the file already exists as a temporary file, it is updated to a permanent file and
     * the list with associated chats is emptied.</li>
     * <li>If temporary is false and the file does not exist, it is stored as a permanent file.</li>
     * </ul>
     * <p>
     * Whether the content of two files is the same is determined by hashing the file content and comparing the hashes.
     * File names are not considered for this comparison.
     *
     * @param iss       InputStreamSource of the file to be stored
     * @param fileName  Name of the file
     * @param temporary Whether the file should be stored as temporary
     * @return UUID of the stored file. It can be used to download the file later or reference it in indexing.
     * @throws ReadFileException        If the file could not be read
     * @throws StorageException         If the file could not be stored
     * @throws SourceCollisionException If a permanent file with the same content already exists
     */
    @Transactional
    public UUID storeFile(InputStreamSource iss, String fileName, boolean temporary)
            throws ReadFileException, StorageException, SourceCollisionException {
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
                        existing.getUsedByChats()
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

    /**
     * Loads a file from the storage by its ID.
     * <p>
     * If the file is found, it returns an Optional containing a DownloadFile object with the file's Resource and
     * filename.
     * If the file is not found, it returns an empty Optional.
     *
     * @param id UUID of the file to be loaded
     * @return Optional containing the DownloadFile if found, otherwise empty
     * @apiNote Files stored with the temporary flag set to true cannot be loaded via this method.
     */
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

    /**
     * A record representing a downloadable file, containing its content in a resource and filename.
     *
     * @param resource The resource containing the file's content
     * @param filename The name of the file
     */
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
