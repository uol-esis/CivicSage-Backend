package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.index.document.readers.WebsiteDocumentReader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final DocumentReaderService documentReaderService;
    private final SemanticSplitterService semanticSplitterService;
    private final EmbeddingService embeddingService;

    public void indexFile(@NonNull MultipartFile file) {
        List<Document> documents = documentReaderService.read(file);
        log.debug("Read {} documents from file: {}", documents.size(), file.getOriginalFilename());
        documents = semanticSplitterService.process(documents);
        log.debug("Split into {} semantic chunks", documents.size());

        embeddingService.save(documents);
    }

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        String url = indexWebsiteRequestDto.getUrl().get();

        List<Document> documents = new WebsiteDocumentReader(url).read();
        log.debug("Read {} documents from url: {}", documents.size(), url);

        embeddingService.save(documents);
    }

}
