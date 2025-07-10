package de.uol.pgdoener.civicsage.index;

import de.uol.pgdoener.civicsage.business.dto.IndexWebsiteRequestDto;
import de.uol.pgdoener.civicsage.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.index.document.DocumentReaderService;
import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.index.exception.StorageException;
import de.uol.pgdoener.civicsage.source.FileHashingService;
import de.uol.pgdoener.civicsage.source.FileSource;
import de.uol.pgdoener.civicsage.source.SourceService;
import de.uol.pgdoener.civicsage.source.WebsiteSource;
import de.uol.pgdoener.civicsage.storage.StorageService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public void indexFile(@NonNull MultipartFile file) {
        String hash = fileHashingService.hash(file);
        sourceService.verifyFileHashNotIndexed(hash);

        List<Document> documents = documentReaderService.read(file);
        log.debug("Read {} documents from file: {}", documents.size(), file.getOriginalFilename());

        documents = postProcessDocuments(documents);
        UUID objectId = storeInStorage(file);
        documents.forEach(document -> document.getMetadata().put(FILE_ID, objectId));

        embeddingService.save(documents);
        sourceService.save(new FileSource(objectId, file.getOriginalFilename(), hash, List.of(modelID)));
    }

    private UUID storeInStorage(MultipartFile file) {
        Optional<UUID> objectID;
        try {
            objectID = storageService.store(file.getInputStream());
            log.info("Stored file {}", file.getOriginalFilename());
        } catch (IOException e) {
            log.error("Error storing file {}", file.getOriginalFilename(), e);
            throw new ReadFileException("Could not read file", e);
        }
        if (objectID.isEmpty()) {
            throw new StorageException("Could not store file");
        }
        return objectID.get();
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
