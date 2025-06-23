package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.document.DocumentReaderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final DocumentReaderService documentReaderService;
    private final SemanticSplitterService semanticSplitterService;
    private final EmbeddingService embeddingService;
    private final TextSplitter textSplitter;

    public void indexFile(@NonNull MultipartFile file, UUID objectId, Map<String, String> metadata) {
        List<Document> documents = documentReaderService.read(file);
        log.debug("Read {} documents from file: {}", documents.size(), file.getOriginalFilename());

        documents = postProcessDocuments(documents);
        documents.forEach(document -> document.getMetadata().put(FILE_ID.getValue(), objectId));
        addMetadataToDocuments(documents, metadata);

        embeddingService.save(documents);
    }

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        String url = indexWebsiteRequestDto.getUrl();

        List<Document> documents = documentReaderService.readURL(url);
        log.debug("Read {} documents from url: {}", documents.size(), url);

        documents = postProcessDocuments(documents);
        addMetadataToDocuments(documents, indexWebsiteRequestDto.getAdditionalMetadata().get().getAdditionalProperties());
        embeddingService.save(documents);
    }

    private List<Document> postProcessDocuments(List<Document> documents) {
        documents = semanticSplitterService.process(documents);
        log.debug("Website split into {} semantic chunks", documents.size());

        documents = documents.stream()
                .flatMap(d -> textSplitter.split(d).stream())
                .toList();
        log.debug("Split into {} chunks to fit context window", documents.size());

        return documents;
    }

    private void addMetadataToDocuments(List<Document> documents, Map<String, String> metadata) {
        if (metadata == null)
            return;
        documents.forEach(document -> {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                document.getMetadata().put(entry.getKey(), entry.getValue());
            }
        });
    }

}
