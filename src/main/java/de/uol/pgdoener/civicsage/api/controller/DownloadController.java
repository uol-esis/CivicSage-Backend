package de.uol.pgdoener.civicsage.api.controller;


import de.uol.pgdoener.civicsage.api.DownloadApi;
import de.uol.pgdoener.civicsage.api.DownloadApiDelegate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadController implements DownloadApiDelegate {

    @Override
    public ResponseEntity<Resource> downloadFile(String fileId) {
        return DownloadApiDelegate.super.downloadFile(fileId);
    }
}
