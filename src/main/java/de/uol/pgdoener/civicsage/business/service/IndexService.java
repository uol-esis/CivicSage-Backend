package de.uol.pgdoener.civicsage.business.service;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class IndexService {

    public void indexFile(MultipartFile file) {
        // Logic to index the file content
        log.error("Indexing file: {}", file.getOriginalFilename());
        log.error("ContentType: {}", file.getContentType());
    }

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        // Logic to index the website content
        log.error("Indexing URL: {}", indexWebsiteRequestDto.getUrl());
    }

}
