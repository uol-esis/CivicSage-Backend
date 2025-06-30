package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.index.exception.StorageException;
import de.uol.pgdoener.civicsage.source.SourceService;
import de.uol.pgdoener.civicsage.source.WebsiteSource;
import de.uol.pgdoener.civicsage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.ADDITIONAL_PROPERTIES;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final SourceService sourceService;
    private final DocumentReaderService documentReaderService;
    private final SemanticSplitterService semanticSplitterService;
    private final EmbeddingService embeddingService;
    private final TextSplitter textSplitter;
    private final StorageService storageService;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelID;

    // ######
    // Files
    // ######

    public void indexFile(IndexFilesRequestInnerDto indexFilesRequestInnerDto) {
        UUID fileId = indexFilesRequestInnerDto.getFileId();
        Map<String, Object> additionalMetadata = indexFilesRequestInnerDto.getAdditionalProperties();

        String fileName = sourceService.getFileSourceById(fileId).getFileName();
        InputStream file = storageService.load(fileId).orElseThrow(() -> new StorageException("Could not load file from storage"));
        List<Document> documents = documentReaderService.read(file, fileName);
        log.debug("Read {} documents from file: {}", documents.size(), fileName);

        documents = postProcessDocuments(documents);
        documents.forEach(document -> {
            document.getMetadata().put(FILE_ID.getValue(), fileId);
            document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), additionalMetadata);
        });


        embeddingService.save(documents);
    }


    // #########
    // Websites
    // #########

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        String url = indexWebsiteRequestDto.getUrl();
        url = normalizeURL(url);
        sourceService.verifyWebsiteNotIndexed(url);

        List<Document> documents = documentReaderService.readURL(url);
        log.debug("Read {} documents from url: {}", documents.size(), url);

        documents = postProcessDocuments(documents);
        documents.forEach(document ->
                document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), indexWebsiteRequestDto.getAdditionalProperties()));
        embeddingService.save(documents);
        sourceService.save(new WebsiteSource(null, url, List.of(modelID)));
    }

    public String normalizeURL(String url) {
        // make sure url starts with a protocol
        if (!url.matches("^[a-z]+://.+")) {
            url = "https://" + url;
        }

        // make sure there is no trailing slash "/"
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    // ########
    // General
    // ########

    private List<Document> postProcessDocuments(List<Document> documents) {
        documents = semanticSplitterService.process(documents);
        log.debug("Website split into {} semantic chunks", documents.size());

        documents = documents.stream()
                .flatMap(d -> textSplitter.split(d).stream())
                .toList();
        log.debug("Split into {} chunks to fit context window", documents.size());

        return documents;
    }

}
