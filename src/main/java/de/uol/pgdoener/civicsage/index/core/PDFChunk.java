package de.uol.pgdoener.civicsage.index.core;

import lombok.NonNull;

import java.util.Optional;

public record PDFChunk(
        @NonNull String content,
        int page,
        int line,
        @NonNull Optional<String> chapter
) implements Chunk {
}
