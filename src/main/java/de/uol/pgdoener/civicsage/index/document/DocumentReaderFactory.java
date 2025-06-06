package de.uol.pgdoener.civicsage.index.document;

import de.uol.pgdoener.civicsage.index.document.readers.PlainTextDocumentReader;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class DocumentReaderFactory {

    public DocumentReader create(@NonNull MultipartFile file, @NonNull String fileEnding) {
        return switch (fileEnding) {
            case "txt" -> {
                log.info("Indexing file as plain text file");
                yield new PlainTextDocumentReader(file);
            }
            case "md", "markdown" -> {
                log.info("Indexing file as Markdown file");
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
                yield new MarkdownDocumentReader(file.getResource(), config);
            }
            case "pdf" -> {
                log.info("Indexing file as PDF file");
                yield new ParagraphPdfDocumentReader(file.getResource());
            }
            default -> throw new ReadFileException("Unsupported file type: " + fileEnding);
        };
    }

}
