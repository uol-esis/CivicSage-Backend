package de.uol.pgdoener.civicsage.index.core;

import lombok.NonNull;

public record PlainTextChunk(
        @NonNull String content,
        int line
) implements Chunk {
}
