package de.uol.pgdoener.civicsage.index.document;

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
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_NAME;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.URL;

@Slf4j
@Component
public class DocumentReaderFactory {

    public DocumentReader create(@NonNull MultipartFile file, @NonNull String fileEnding) {
        return switch (fileEnding) {
            case "txt" -> {
                log.info("Indexing file as plain text file");
                TextReader reader = new TextReader(file.getResource());
                reader.getCustomMetadata().put(FILE_NAME, file.getOriginalFilename());
                yield reader;
            }
            case "md", "markdown" -> {
                log.info("Indexing file as Markdown file");
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
                config.additionalMetadata.put(FILE_NAME, file.getOriginalFilename());
                yield new MarkdownDocumentReader(file.getResource(), config);
            }
            case "pdf" -> {
                log.info("Indexing file as PDF file");
                yield new ParagraphPdfDocumentReader(file.getResource());
            }
            // https://tika.apache.org/3.1.0/formats.html
            case "odt", "odp", "ods", // LibreOffice
                 "doc", "docx", "pptx", "xls", "xlsx" // Microsoft Office
                    -> {
                log.info("Indexing file with Tika");
                yield new TikaDocumentReader(file.getResource()) {
                    @NonNull
                    @Override
                    public List<Document> read() {
                        return super.read().stream()
                                .peek(d -> d.getMetadata().put(FILE_NAME, file.getOriginalFilename()))
                                .toList();
                    }
                };
            }
            default -> throw new ReadFileException("Unsupported file type: " + fileEnding);
        };
    }

    public DocumentReader createForURL(@NonNull String url) {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .additionalMetadata(URL, url)
                .build();
        return new JsoupDocumentReader(url, config);
    }

}
