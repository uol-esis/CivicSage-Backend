package de.uol.pgdoener.civicsage.business.infrastructure.index.chunk.chunkers;

import de.uol.pgdoener.civicsage.business.infrastructure.index.core.ChunkedFile;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

public class PlainTextChunker implements Chunker {

    @Override
    public ChunkedFile chunk(@NonNull MultipartFile file) {
        file.
    }

}
