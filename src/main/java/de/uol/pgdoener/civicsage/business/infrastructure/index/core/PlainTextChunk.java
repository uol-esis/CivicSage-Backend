package de.uol.pgdoener.civicsage.business.infrastructure.index.core;

import lombok.NonNull;

public record PlainTextChunk(
        @NonNull String content,
        int line
) implements Chunk {
}
