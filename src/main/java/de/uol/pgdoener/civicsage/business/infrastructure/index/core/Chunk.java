package de.uol.pgdoener.civicsage.business.infrastructure.index.core;

public sealed interface Chunk permits
        PDFChunk,
        PlainTextChunk {
}
