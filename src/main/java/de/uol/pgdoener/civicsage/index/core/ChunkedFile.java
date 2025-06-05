package de.uol.pgdoener.civicsage.index.core;

import lombok.NonNull;

import java.util.List;

public record ChunkedFile(
        @NonNull String name,
        @NonNull List<? extends Chunk> chunks
) {
}
