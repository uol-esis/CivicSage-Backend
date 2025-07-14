package de.uol.pgdoener.civicsage.business.index;

import de.uol.pgdoener.civicsage.business.dto.IndexFilesRequestInnerDto;
import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.business.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.business.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.business.index.document.MetadataKeys;
import de.uol.pgdoener.civicsage.business.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.business.index.exception.SplittingException;
import de.uol.pgdoener.civicsage.business.index.exception.StorageException;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.source.WebsiteSource;
import de.uol.pgdoener.civicsage.business.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.business.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.uol.pgdoener.civicsage.business.index.document.MetadataKeys.ADDITIONAL_PROPERTIES;
import static de.uol.pgdoener.civicsage.business.index.document.MetadataKeys.FILE_ID;

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
        final Map<String, Object> additionalMetadata = indexFilesRequestInnerDto.getAdditionalProperties() == null ?
                new HashMap<>() : indexFilesRequestInnerDto.getAdditionalProperties();

        // Verify that the file is not already indexed for the current model
        FileSource fileSource = sourceService.getFileSourceById(fileId);
        if (fileSource.getModels().contains(modelID)) {
            throw new SourceCollisionException("File is already indexed for current model!");
        }

        // Read the file from storage and process it
        String fileName = fileSource.getFileName();
        InputStream file = storageService.load(fileId).orElseThrow(() -> new StorageException("Could not load file from storage"));
        Resource resource = toResource(file, fileName);
        List<Document> documents = documentReaderService.read(resource, fileName);
        log.debug("Read {} documents from file: {}", documents.size(), fileName);

        documents = postProcessDocuments(documents);
        documents.forEach(document -> {
            document.getMetadata().put(FILE_ID.getValue(), fileId);
            document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), additionalMetadata);
        });

        // Update the file source with the new model ID
        fileSource.getModels().add(modelID);
        fileSource.getMetadata().putAll(getMetadataFromDocuments(documents));
        sourceService.save(fileSource);

        embeddingService.save(documents);
    }

    private Resource toResource(InputStream inputStream, String fileName) {
        try {
            byte[] data = inputStream.readAllBytes();
            return new ByteArrayResource(data) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
        } catch (IOException e) {
            throw new ReadFileException("Could not read PDF file: " + fileName, e);
        }
    }


    // #########
    // Websites
    // #########

    public void indexURL(IndexWebsiteRequestDto indexWebsiteRequestDto) {
        String url = indexWebsiteRequestDto.getUrl();
        url = normalizeURL(url);
        final Map<String, Object> additionalProperties = indexWebsiteRequestDto.getAdditionalProperties() == null ?
                new HashMap<>() : indexWebsiteRequestDto.getAdditionalProperties();

        WebsiteSource websiteSource = sourceService.getWebsiteSourceByUrl(url)
                .orElse(new WebsiteSource(null, url, new ArrayList<>(), new HashMap<>()));
        if (websiteSource.getModels().contains(modelID)) {
            throw new SourceCollisionException("Website is already indexed for current model!");
        }

        List<Document> documents = documentReaderService.readURL(url);
        log.debug("Read {} documents from url: {}", documents.size(), url);

        documents = postProcessDocuments(documents);
        documents.forEach(document ->
                document.getMetadata().put(ADDITIONAL_PROPERTIES.getValue(), additionalProperties));

        websiteSource.getModels().add(modelID);
        websiteSource.getMetadata().putAll(getMetadataFromDocuments(documents));
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

        final int numDocumentsBeforeSplitting = documents.size();
        documents = documents.stream()
                .flatMap(d -> textSplitter.split(d).stream())
                .toList();
        log.debug("Split into {} chunks to fit context window", documents.size());

        if (documents.isEmpty())
            throw new SplittingException("Source does not have enough content to be indexed");
        if (documents.size() < numDocumentsBeforeSplitting)
            log.warn("There are less documents after splitting than before.");

        return documents;
    }

    /**
     * This method extracts the metadata from the first document in the list.
     * It filters the metadata keys to only include those that are exposed via the API.
     *
     * @param documents the list of documents to extract metadata from
     * @return a map of metadata keys and values that are exposed via the API
     */
    private Map<String, Object> getMetadataFromDocuments(List<Document> documents) {
        final Map<String, Object> metadataOfFirstDocument = documents.getFirst().getMetadata();
        Map<String, Object> exposedMetadata = MetadataKeys.EXPOSED_KEYS.stream()
                .map(MetadataKeys::getValue)
                .filter(metadataOfFirstDocument::containsKey)
                .collect(Collectors.toMap(
                        Function.identity(),
                        metadataOfFirstDocument::get
                ));
        if (metadataOfFirstDocument.containsKey(ADDITIONAL_PROPERTIES.getValue()))
            exposedMetadata.put(
                    ADDITIONAL_PROPERTIES.getValue(),
                    metadataOfFirstDocument.get(ADDITIONAL_PROPERTIES.getValue())
            );
        return exposedMetadata;
    }

}
