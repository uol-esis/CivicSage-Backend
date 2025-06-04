package de.uol.pgdoener.civicsage.business.infrastructure.index.chunk;

import de.uol.pgdoener.civicsage.business.infrastructure.index.chunk.chunkers.Chunker;
import de.uol.pgdoener.civicsage.business.infrastructure.index.chunk.chunkers.PlainTextChunker;
import de.uol.pgdoener.civicsage.business.infrastructure.index.exception.ReadFileException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChunkerFactory {

    public Chunker create(@NonNull String fileEnding) {
        return switch (fileEnding) {
            case "txt" -> {
                log.info("Indexing file as plain text file");
                yield new PlainTextChunker();
            }
            case "pdf" -> {
                log.info("Indexing file as PDF file");
                throw new NotImplementedException();
            }
            default -> throw new ReadFileException("Unsupported file type: " + fileEnding);
        };
    }

}
