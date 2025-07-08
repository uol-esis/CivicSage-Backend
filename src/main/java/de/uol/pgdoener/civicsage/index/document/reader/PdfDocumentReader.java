package de.uol.pgdoener.civicsage.index.document.reader;

import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class PdfDocumentReader implements DocumentReader {

    private final Resource resource;

    @Override
    public List<Document> get() {
        byte[] data;
        try {
            data = resource.getContentAsByteArray();
        } catch (IOException e) {
            throw new ReadFileException("Could not read PDF file: " + resource.getFilename(), e);
        }

        try {
            ParagraphPdfDocumentReader reader = new ParagraphPdfDocumentReader(constructRessourceFromData(data));
            return reader.read();
        } catch (RuntimeException e) {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(constructRessourceFromData(data));
            return reader.read();
        }
    }

    private Resource constructRessourceFromData(byte[] data) {
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return resource.getFilename();
            }
        };
    }

}
