package de.uol.pgdoener.civicsage.index.core;

public sealed interface Chunk permits
        PDFChunk,
        PlainTextChunk {
}
