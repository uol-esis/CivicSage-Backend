package de.uol.pgdoener.civicsage.mapper;

import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import de.uol.pgdoener.civicsage.index.document.MetadataKeys;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.*;

@Slf4j
@Component
public class SearchResultMapper {

    public List<SearchResultDto> toDto(@NonNull List<Document> documents) {
        return documents.stream()
                .map(this::toDto)
                .toList();
    }

    public SearchResultDto toDto(@NonNull Document document) {
        SearchResultDto searchResultDto = new SearchResultDto();
        searchResultDto.setScore(document.getScore());
        searchResultDto.setText(document.getText());

        Map<String, Object> metadata = document.getMetadata();
        Object fileName = metadata.get(FILE_NAME.getValue());
        Object url = metadata.get(URL.getValue());

        if (fileName instanceof String file) {
            searchResultDto.fileName(file);
            if (metadata.get(FILE_ID.getValue()) instanceof String fileIdString)
                searchResultDto.fileId(UUID.fromString(fileIdString));
            if (metadata.get(TITLE.getValue()) instanceof String t)
                searchResultDto.title(t);
            else
                searchResultDto.title(constructTitleForFile(file));
        } else if (url instanceof String u) {
            searchResultDto.url(u);
            if (metadata.get(TITLE.getValue()) instanceof String t)
                searchResultDto.title(t);
        } else {
            log.error("Document is neither a file nor a url: {}", document);
        }

        // add all remaining metadata entries
        metadata
                .entrySet()
                .stream()
                .filter(entry ->
                        !Arrays.stream(MetadataKeys.values())
                                .map(MetadataKeys::getValue)
                                .toList()
                                .contains(entry.getKey()))
                .forEach(entry ->
                        searchResultDto
                                .getAdditionalProperties()
                                .put(entry.getKey(), entry.getValue().toString()));

        return searchResultDto;
    }

    private String constructTitleForFile(String fileName) {
        String title = fileName;

        int indexOfFileEnding = title.lastIndexOf(".");
        if (indexOfFileEnding != -1) {
            title = title.substring(0, indexOfFileEnding);
        }

        return title.replace('_', ' ');
    }

}
