package de.uol.pgdoener.civicsage.storage;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
public interface StorageService {

    /**
     * Uploads a given inputStream to the objectStorage
     *
     * @param inputStream InputStream of the object to store
     * @return Optional with the UUID of the stored object if the upload was successful, empty Optional otherwise.
     */
    Optional<UUID> store(InputStream inputStream);

    /**
     * @param objectID UUID of the object to load
     * @return Optional with the InputStream of the object if it was found, empty Optional otherwise.
     */
    Optional<InputStream> load(UUID objectID);

}
