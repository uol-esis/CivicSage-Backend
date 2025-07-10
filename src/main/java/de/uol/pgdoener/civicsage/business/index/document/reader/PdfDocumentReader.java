package de.uol.pgdoener.civicsage.business.index.document.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.core.io.Resource;

import java.util.List;

@RequiredArgsConstructor
public class PdfDocumentReader implements DocumentReader {

    private final Resource resource;

    @Override
    public List<Document> get() {
        try {
            ParagraphPdfDocumentReader reader = new ParagraphPdfDocumentReader(resource);
            return reader.read();
        } catch (RuntimeException e) {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            return reader.read();
        }
    }

}
