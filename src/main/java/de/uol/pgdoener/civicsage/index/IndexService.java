package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.index.exception.StorageException;
import de.uol.pgdoener.civicsage.source.FileHashingService;
import de.uol.pgdoener.civicsage.source.FileSource;
import de.uol.pgdoener.civicsage.source.SourceService;
import de.uol.pgdoener.civicsage.source.WebsiteSource;
import de.uol.pgdoener.civicsage.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
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
    private final FileHashingService fileHashingService;
    private final StorageService storageService;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelID;

    // ######
    // Files
    // ######

    public void indexFile(IndexFilesRequestInnerDto indexFilesRequestInnerDto) {
        UUID fileRef = indexFilesRequestInnerDto.getFileId();
        String fileName = indexFilesRequestInnerDto.getName();
        Map<String, Object> additionalMetadata = indexFilesRequestInnerDto.getAdditionalProperties();

        // Verify that the file is not already indexed for the current model
        FileSource fileSource = sourceService.getFileSourceById(fileRef);
        if (fileSource.getModels().contains(modelID)) {
            throw new SourceCollisionException("File is already indexed for current model!");
        }

        // Read the file from storage and process it
        InputStream file = storageService.load(fileRef).orElseThrow(() -> new StorageException("Could not load file from storage"));
        List<Document> documents = documentReaderService.read(file, fileName);
        log.debug("Read {} documents from file: {}", documents.size(), fileName);

        documents = postProcessDocuments(documents);
        documents.forEach(document -> {
            document.getMetadata().put(FILE_ID.getValue(), fileRef);
            document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), additionalMetadata);
        });

        // Update the file source with the new model ID
        fileSource.getModels().add(modelID);
        sourceService.save(fileSource);

        embeddingService.save(documents);
    }


    // #########
    // Websites
    // #########

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        String url = indexWebsiteRequestDto.getUrl();
        url = normalizeURL(url);

        WebsiteSource websiteSource = sourceService.getWebsiteSourceByUrl(url)
                .orElse(new WebsiteSource(null, url, new ArrayList<>()));
        if (websiteSource.getModels().contains(modelID)) {
            throw new SourceCollisionException("Website is already indexed for current model!");
        }

        List<Document> documents = documentReaderService.readURL(url);
        log.debug("Read {} documents from url: {}", documents.size(), url);

        documents = postProcessDocuments(documents);
        documents.forEach(document ->
                document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), indexWebsiteRequestDto.getAdditionalProperties()));

        websiteSource.getModels().add(modelID);
        sourceService.save(websiteSource);

        embeddingService.save(documents);
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
