package de.uol.pgdoener.civicsage.index.document;

import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.index.exception.ReadUrlException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentReaderService {

    private final DocumentReaderFactory documentReaderFactory;

    public List<Document> read(@NonNull Resource file, @NonNull String fileName) {
        if (fileName.isBlank())
            throw new ReadFileException("File name is empty or null");
        final String fileEnding = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        DocumentReader documentReader = documentReaderFactory.create(file, fileEnding, fileName);
        return documentReader.read();
    }

    public List<Document> readURL(String url) {
        // can only read urls using HTTP(S)
        //noinspection HttpUrlsUsage
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new ReadUrlException("Invalid protocol used in URL: " + url);
        }

        DocumentReader documentReader = documentReaderFactory.createForURL(url);
        List<Document> documents;
        try {
            documents = documentReader.read();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof UnknownHostException)
                throw new ReadUrlException("Unknown host");
            if (e.getCause() instanceof FileNotFoundException)
                throw new ReadUrlException("Website not found");
            if (e.getCause() instanceof IllegalArgumentException illegalArgumentException)
                throw new ReadUrlException(illegalArgumentException.getMessage());
            throw new ReadUrlException("Unknown error while reading URL", e);
        }
        return documents;
    }

}
