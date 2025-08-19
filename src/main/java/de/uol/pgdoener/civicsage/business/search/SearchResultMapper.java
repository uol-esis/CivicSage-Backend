package de.uol.pgdoener.civicsage.business.search;

import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.uol.pgdoener.civicsage.business.index.document.MetadataKeys.*;

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
        searchResultDto.documentId(UUID.fromString(document.getId()));
        searchResultDto.setScore(getScore(document));
        searchResultDto.setText(document.getText());
        String uploadDateValue = (String) document.getMetadata().get(UPLOAD_DATE.getValue());
        OffsetDateTime uploadDate = OffsetDateTime.parse(uploadDateValue);
        searchResultDto.uploadDate(uploadDate);

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

        @SuppressWarnings("unchecked") // Cast is safe because we control the metadata structure!
        Map<String, Object> addProps = (Map<String, Object>) metadata.get(ADDITIONAL_PROPERTIES.getValue());
        if (addProps != null) {
            addProps.forEach(searchResultDto::putAdditionalProperty);
        }

        return searchResultDto;
    }

    // This method is necessary because the MariaDB vector store does not return a score by default.
    // Instead, it returns a distance, which we convert to a score.
    private double getScore(@NonNull Document document) {
        Double score = document.getScore();
        if (score == null) {
            Object distance = document.getMetadata().get("distance");
            if (distance instanceof Number d) {
                score = 1.0 - d.doubleValue();
            } else {
                log.error("Document has no score and no distance: {}", document);
                score = 0.0; // Default score if no score is available
            }
        }
        return score;
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
