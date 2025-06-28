package de.uol.pgdoener.civicsage.index.document;

import de.uol.pgdoener.civicsage.index.document.reader.PdfDocumentReader;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_NAME;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.URL;

@Slf4j
@Component
public class DocumentReaderFactory {

    public DocumentReader create(@NonNull InputStream file, @NonNull String fileEnding, @NonNull String fileName) {
        // FIXME: InputStreamResource is not suitable for some readers (PDF Reader for example) as they expect more
        // information from a Resource - e.g. the filename
        Resource resource = new InputStreamResource(file) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        return switch (fileEnding) {
            case "txt" -> {
                log.info("Indexing file as plain text file");
                TextReader reader = new TextReader(resource);
                reader.getCustomMetadata().put(FILE_NAME.getValue(), fileName);
                yield reader;
            }
            case "md", "markdown" -> {
                log.info("Indexing file as Markdown file");
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
                config.additionalMetadata.put(FILE_NAME.getValue(), fileName);
                yield new MarkdownDocumentReader(resource, config);
            }
            case "pdf" -> {
                log.info("Indexing file as PDF file");
                yield new PdfDocumentReader(resource);
            }
            // https://tika.apache.org/3.1.0/formats.html
            case "odt", "odp", "ods", // LibreOffice
                 "doc", "docx", "pptx", "xls", "xlsx" // Microsoft Office
                    -> {
                log.info("Indexing file with Tika");
                yield new TikaDocumentReader(resource) {
                    @NonNull
                    @Override
                    public List<Document> read() {
                        return super.read().stream()
                                .peek(d -> d.getMetadata().put(FILE_NAME.getValue(), fileName))
                                .toList();
                    }
                };
            }
            default -> throw new ReadFileException("Unsupported file type: " + fileEnding);
        };
    }

    public DocumentReader createForURL(@NonNull String url) {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .additionalMetadata(URL.getValue(), url)
                .build();
        return new JsoupDocumentReader(url, config);
    }

}
