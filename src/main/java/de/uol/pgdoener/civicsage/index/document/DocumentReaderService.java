package de.uol.pgdoener.civicsage.index.document;

import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.index.exception.ReadUrlException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentReaderService {

    private final DocumentReaderFactory documentReaderFactory;

    public List<Document> read(@NonNull MultipartFile file) {
        final String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty())
            throw new ReadFileException("File name is empty or null");
        final String fileEnding = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        DocumentReader documentReader = documentReaderFactory.create(file, fileEnding);
        return documentReader.read();
    }

    public List<Document> readURL(String url) {
        // make sure url starts with http(s)://
        if (url.matches("^[a-z]+://.+")) {
            //noinspection HttpUrlsUsage
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                throw new ReadUrlException("Invalid protocol used in URL: " + url);
            }
        } else {
            url = "https://" + url;
        }

        DocumentReader documentReader = documentReaderFactory.createForURL(url);
        return documentReader.read();
    }

}
