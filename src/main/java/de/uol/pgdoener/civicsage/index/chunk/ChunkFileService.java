package de.uol.pgdoener.civicsage.index.chunk;

import de.uol.pgdoener.civicsage.index.chunk.chunkers.Chunker;
import de.uol.pgdoener.civicsage.index.core.ChunkedFile;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChunkFileService {

    private final ChunkerFactory chunkerFactory;

    public ChunkedFile process(@NonNull MultipartFile file) {
        final String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty())
            throw new ReadFileException("File name is empty or null");
        final String fileEnding = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        Chunker chunker = chunkerFactory.create(fileEnding);
        return chunker.chunk(file);
    }

}
