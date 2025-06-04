package de.uol.pgdoener.civicsage.business.infrastructure.index.core;

import lombok.NonNull;

import java.util.List;

public record ChunkedFile(
        @NonNull String name,
        @NonNull List<Chunk> chunks
) {
}
