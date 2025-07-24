package de.uol.pgdoener.civicsage.business.storage.impl;

import de.uol.pgdoener.civicsage.autoconfigure.S3Properties;
import de.uol.pgdoener.civicsage.business.storage.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * @see <a href=https://gurselgazii.medium.com/integrating-minio-with-spring-boot-a-guide-to-simplified-object-storage-525d5a7686cc>medium.com</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final S3Properties s3Properties;
    private final MinioClient minioClient;

    @Override
    public Optional<UUID> store(InputStream inputStream) {
        UUID objectID = UUID.randomUUID();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(s3Properties.getBucket().getName())
                    .object(objectID.toString())
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
            log.debug("Stored object in objectStorage");
            return Optional.of(objectID);
        } catch (Exception e) {
            log.warn("Upload failed", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> load(UUID objectID) {
        try {
            var response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(s3Properties.getBucket().getName())
                    .object(objectID.toString())
                    .build());
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Download failed", e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(UUID objectID) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(s3Properties.getBucket().getName())
                    .object(objectID.toString())
                    .build());
            log.debug("Deleted object with ID: {}", objectID);
        } catch (Exception e) {
            log.warn("Failed to delete object with ID: {}", objectID, e);
        }
    }

    @PostConstruct
    private void ensureBucketExistence() {
        log.warn("Bucket existence not ensured yet!");
        // FIXME implement this
    }
}
