package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.SourcesApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.GetAllIndexedSources200ResponseDto;
import de.uol.pgdoener.civicsage.business.dto.UpdateIndexedWebsiteRequestDto;
import de.uol.pgdoener.civicsage.business.embedding.EmbeddingService;
import de.uol.pgdoener.civicsage.business.index.IndexService;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceMapper;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.source.WebsiteSource;
import de.uol.pgdoener.civicsage.business.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SourcesController implements SourcesApiDelegate {

    private final SourceService sourceService;
    private final SourceMapper sourceMapper;
    private final EmbeddingService embeddingService;
    private final StorageService storageService;
    private final IndexService indexService;

    @Override
    public ResponseEntity<GetAllIndexedSources200ResponseDto> getAllIndexedSources(Optional<String> filterExpression) {
        GetAllIndexedSources200ResponseDto response = new GetAllIndexedSources200ResponseDto();

        final Collection<UUID> pendingSourceIds = embeddingService.getPendingSourceIds();

        Iterable<FileSource> fileSources = sourceService.getAllFileSources(filterExpression.orElse(""));
        for (FileSource fileSource : fileSources) {
            boolean embedded = !pendingSourceIds.contains(fileSource.getObjectStorageId());
            response.addFilesItem(sourceMapper.toDto(fileSource, embedded));
        }

        Iterable<WebsiteSource> websiteSources = sourceService.getAllWebsiteSources(filterExpression.orElse(""));
        for (WebsiteSource websiteSource : websiteSources) {
            boolean embedded = !pendingSourceIds.contains(websiteSource.getId());
            response.addWebsitesItem(sourceMapper.toDto(websiteSource, embedded));
        }

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteIndexedSource(UUID id) {
        embeddingService.delete(id);
        if (!sourceService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<FileSource> fileSource = sourceService.getFileSourceByIdWithTemporary(id);
        if (fileSource.isPresent() && !fileSource.get().isTemporary() && !fileSource.get().getUsedByChats().isEmpty()) {
            FileSource newFileSource = new FileSource(
                    UUID.randomUUID(),
                    fileSource.get().getFileName(),
                    fileSource.get().getHash(),
                    fileSource.get().getUploadDate(),
                    fileSource.get().getModels(),
                    fileSource.get().getMetadata(),
                    true,
                    fileSource.get().getUsedByChats()
            );
            sourceService.save(newFileSource);
        } else {
            sourceService.deleteSource(id);
            storageService.delete(id);
        }
        return ResponseEntity.status(204).build();
    }

    @Override
    public ResponseEntity<Void> updateIndexedWebsite(UpdateIndexedWebsiteRequestDto updateIndexedWebsiteRequestDto) {
        List<UUID> ids = updateIndexedWebsiteRequestDto.getIds();

        indexService.updateWebsites(ids);

        return ResponseEntity.status(202).build();
    }
}
