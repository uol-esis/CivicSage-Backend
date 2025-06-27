package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.FilesApi;
import de.uol.pgdoener.civicsage.business.dto.UploadFile200ResponseDto;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.index.exception.StorageException;
import de.uol.pgdoener.civicsage.source.FileHashingService;
import de.uol.pgdoener.civicsage.source.FileSource;
import de.uol.pgdoener.civicsage.source.SourceService;
import de.uol.pgdoener.civicsage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FilesController implements FilesApi {

    private final StorageService storageService;
    private final SourceService sourceService;
    private final FileHashingService fileHashingService;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelID;

    @Override
    public ResponseEntity<Resource> downloadFile(UUID id) {
        log.info("Looking for file with id {} in ObjectStorage", id);
        Optional<InputStream> optionalInputStream = storageService.load(id);
        if (optionalInputStream.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource inputStreamResource = new InputStreamResource(optionalInputStream.get());
        String fileName = sourceService.getFileSourceById(id).getFileName();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(fileName)
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.debug("Returning file as download");
        return ResponseEntity.ok()
                .headers(headers)
                .body(inputStreamResource);
    }

    @Override
    public ResponseEntity<UploadFile200ResponseDto> uploadFile(MultipartFile file) {
        UUID objectID = storeInStorage(file);
        try {
            String hash = fileHashingService.hash(file.getInputStream());
            sourceService.verifyFileHashNotIndexed(hash);
            sourceService.save(new FileSource(objectID, file.getOriginalFilename(), hash, List.of(modelID)));
            log.info("File {} uploaded successfully with ID {}", file.getOriginalFilename(), objectID);
            UploadFile200ResponseDto response = new UploadFile200ResponseDto();
            response.setId(objectID);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new ReadFileException("Could not read file.", e);
        }
    }

    private UUID storeInStorage(MultipartFile file) {
        Optional<UUID> objectID;
        try {
            objectID = storageService.store(file.getInputStream());
            log.info("Stored file {}", file.getOriginalFilename());
        } catch (IOException e) {
            log.error("Error storing file {}", file.getOriginalFilename(), e);
            throw new ReadFileException("Could not read file", e);
        }
        if (objectID.isEmpty()) {
            throw new StorageException("Could not store file");
        }
        return objectID.get();
    }

}
