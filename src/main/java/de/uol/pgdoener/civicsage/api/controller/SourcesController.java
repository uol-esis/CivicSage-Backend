package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.SourcesApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.GetAllIndexedSources200ResponseDto;
import de.uol.pgdoener.civicsage.business.source.FileSource;
import de.uol.pgdoener.civicsage.business.source.SourceMapper;
import de.uol.pgdoener.civicsage.business.source.SourceService;
import de.uol.pgdoener.civicsage.business.source.WebsiteSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SourcesController implements SourcesApiDelegate {

    private final SourceService sourceService;
    private final SourceMapper sourceMapper;

    @Override
    public ResponseEntity<GetAllIndexedSources200ResponseDto> getAllIndexedSources(Optional<String> filterExpression) {
        GetAllIndexedSources200ResponseDto response = new GetAllIndexedSources200ResponseDto();

        Iterable<FileSource> fileSources = sourceService.getAllFileSources(filterExpression.orElse(""));
        for (FileSource fileSource : fileSources) {
            response.addFilesItem(sourceMapper.toDto(fileSource));
        }

        Iterable<WebsiteSource> websiteSources = sourceService.getAllWebsiteSources(filterExpression.orElse(""));
        for (WebsiteSource websiteSource : websiteSources) {
            response.addWebsitesItem(sourceMapper.toDto(websiteSource));
        }

        return ResponseEntity.ok(response);
    }

}
