package de.uol.pgdoener.civicsage.index.document.readers;

import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PlainTextDocumentReader implements DocumentReader {

    private final MultipartFile file;

    @Override
    public List<Document> get() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            return readFileInDocuments(reader);
        } catch (IOException e) {
            throw new ReadFileException("Could not read file", e);
        }
    }

    private List<Document> readFileInDocuments(BufferedReader reader) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new ReadFileException("File name is empty or null");
        }
        List<Document> documents = new ArrayList<>();

        String line;
        int lineNumber = 0;
        // Read the file line by line and create documents for each non-empty line
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (!line.isBlank()) { // Skip empty lines
                documents.add(createDocument(line, lineNumber));
            }
        }

        return documents;
    }

    private Document createDocument(String content, int lineNumber) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file_name", file.getOriginalFilename());
        metadata.put("line_number", lineNumber);

        return new Document(content, metadata);
    }

}
