package de.uol.pgdoener.civicsage.index.chunk.chunkers;

import de.uol.pgdoener.civicsage.index.core.ChunkedFile;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

public interface Chunker {

    ChunkedFile chunk(@NonNull MultipartFile file);

}
