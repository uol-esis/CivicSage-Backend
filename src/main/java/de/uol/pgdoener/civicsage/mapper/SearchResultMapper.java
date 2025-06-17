package de.uol.pgdoener.civicsage.mapper;

import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_NAME;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.URL;

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
        Object fileName = metadata.get(FILE_NAME);
        Object url = metadata.get(URL);

        if (fileName instanceof String file) {
            searchResultDto.fileName(file);
            searchResultDto.fileId(toFileRef(metadata));
        } else if (url instanceof String u) {
            searchResultDto.url(u);
        } else {
            log.error("Document is neither a file nor a url: {}", document);
        }

        return searchResultDto;
    }

    private UUID toFileRef(Map<String, Object> metadata) {
        return UUID.randomUUID();
    }

}
