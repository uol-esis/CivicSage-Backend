package de.uol.pgdoener.civicsage.index.chunk.chunkers;

import de.uol.pgdoener.civicsage.index.core.ChunkedFile;
import de.uol.pgdoener.civicsage.index.core.PlainTextChunk;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlainTextChunker implements Chunker {

    @Override
    public ChunkedFile chunk(@NonNull MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            return readFileInChunks(file, reader);
        } catch (IOException e) {
            throw new ReadFileException("Could not read file", e);
        }
    }

    private static ChunkedFile readFileInChunks(MultipartFile file, BufferedReader reader) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new ReadFileException("File name is empty or null");
        }
        List<PlainTextChunk> chunks = new ArrayList<>();

        String line;
        int lineNumber = 0;
        // Read the file line by line and create chunks for each non-empty line
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (!line.isBlank()) { // Skip empty lines
                chunks.add(new PlainTextChunk(line, lineNumber));
            }
        }

        return new ChunkedFile(file.getOriginalFilename(), chunks);
    }

}
