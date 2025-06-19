package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.FilesApi;
import de.uol.pgdoener.civicsage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FilesController implements FilesApi {

    private final StorageService storageService;

    @Override
    public ResponseEntity<Resource> downloadFile(UUID id) {
        log.info("Looking for file with id {} in ObjectStorage", id);
        Optional<InputStream> optionalInputStream = storageService.load(id);
        if (optionalInputStream.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource inputStreamResource = new InputStreamResource(optionalInputStream.get());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("fileName") // TODO retrieve original file name from database
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // TODO test if content length has to be set here

        log.debug("Returning file as download");
        return ResponseEntity.ok()
                .headers(headers)
                .body(inputStreamResource);
    }

}
