package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.FilesApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.UploadFile200ResponseDto;
import de.uol.pgdoener.civicsage.business.storage.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilesController implements FilesApiDelegate {

    private final FileService fileService;

    @Override
    public ResponseEntity<Resource> downloadFile(UUID id) {
        Optional<FileService.DownloadFile> optionalResult = fileService.loadFile(id);
        if (optionalResult.isEmpty())
            return ResponseEntity.notFound().build();
        FileService.DownloadFile file = optionalResult.get();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(file.filename())
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        log.debug("Returning file as download");
        return ResponseEntity.ok()
                .headers(headers)
                .body(file.resource());
    }

    @Override
    public ResponseEntity<UploadFile200ResponseDto> uploadFile(MultipartFile file, Optional<Boolean> temporary) {
        UUID objectID = fileService.storeFile(file, file.getOriginalFilename(), temporary.orElse(false));
        UploadFile200ResponseDto response = new UploadFile200ResponseDto();
        response.setId(objectID);
        return ResponseEntity.ok(response);
    }

}
