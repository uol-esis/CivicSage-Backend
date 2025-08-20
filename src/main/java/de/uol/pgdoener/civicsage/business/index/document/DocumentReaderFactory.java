package de.uol.pgdoener.civicsage.business.index.document;

import de.uol.pgdoener.civicsage.business.index.document.reader.PdfDocumentReader;
import de.uol.pgdoener.civicsage.business.index.document.reader.WebsiteDocumentReader;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.uol.pgdoener.civicsage.business.index.document.MetadataKeys.FILE_NAME;
import static de.uol.pgdoener.civicsage.business.index.document.MetadataKeys.URL;

@Slf4j
@Component
public class DocumentReaderFactory {

    public DocumentReader create(@NonNull Resource file, @NonNull String fileEnding, @NonNull String fileName) {
        return switch (fileEnding) {
            case "txt" -> {
                log.info("Indexing file as plain text file");
                TextReader reader = new TextReader(file);
                reader.getCustomMetadata().put(FILE_NAME.getValue(), fileName);
                yield reader;
            }
            case "md", "markdown" -> {
                log.info("Indexing file as Markdown file");
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
                config.additionalMetadata.put(FILE_NAME.getValue(), fileName);
                yield new MarkdownDocumentReader(file, config);
            }
            case "pdf" -> {
                log.info("Indexing file as PDF file");
                yield new PdfDocumentReader(file);
            }
            // https://tika.apache.org/3.1.0/formats.html
            case "odt", "odp", "ods", // LibreOffice
                 "doc", "docx", "pptx", "xls", "xlsx" // Microsoft Office
                    -> {
                log.info("Indexing file with Tika");
                yield new TikaDocumentReader(file) {
                    @NonNull
                    @Override
                    public List<Document> read() {
                        List<Document> documents = super.read().stream()
                                .toList();
                        documents.forEach(d -> d.getMetadata().put(FILE_NAME.getValue(), fileName));
                        return documents;
                    }
                };
            }
            default -> throw new ReadFileException("Unsupported file type: " + fileEnding);
        };
    }

    public DocumentReader createForURL(@NonNull String url, @NonNull Resource resource) {
        WebsiteDocumentReader reader = new WebsiteDocumentReader(resource);
        reader.getAdditionalMetadata().put(URL.getValue(), url);
        return reader;
    }

}
