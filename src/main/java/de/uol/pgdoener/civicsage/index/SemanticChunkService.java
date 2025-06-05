package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.index.core.ChunkedFile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticChunkService {

    public ChunkedFile process(@NonNull ChunkedFile chunkedFile) {
        // TODO split read chunks into semantically meaningful chunks
        return chunkedFile;
    }

}
