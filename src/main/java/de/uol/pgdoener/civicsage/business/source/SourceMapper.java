package de.uol.pgdoener.civicsage.business.source;

import de.uol.pgdoener.civicsage.business.dto.FileSourceDto;
import de.uol.pgdoener.civicsage.business.dto.WebsiteSourceDto;
import de.uol.pgdoener.civicsage.business.index.document.MetadataKeys;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SourceMapper {

    public FileSourceDto toDto(@NonNull FileSource fileSource, boolean embedded) {
        FileSourceDto dto = new FileSourceDto()
                .fileId(fileSource.getObjectStorageId())
                .fileName(fileSource.getFileName());
        dto.setTitle((String) fileSource.getMetadata().get(MetadataKeys.TITLE.getValue()));
        //noinspection unchecked
        ((Map<String, Object>) fileSource.getMetadata().getOrDefault(MetadataKeys.ADDITIONAL_PROPERTIES.getValue(), Map.of()))
                .forEach((key, value) -> {
                    if (!MetadataKeys.TITLE.getValue().equals(key))
                        dto.putAdditionalProperty(key, value);
                });
        dto.putAdditionalProperty("embedded", embedded);
        return dto;
    }

    public WebsiteSourceDto toDto(@NonNull WebsiteSource websiteSource, boolean embedded) {
        WebsiteSourceDto dto = new WebsiteSourceDto()
                .websiteId(websiteSource.getId())
                .url(websiteSource.getUrl());
        dto.setTitle((String) websiteSource.getMetadata().get(MetadataKeys.TITLE.getValue()));
        //noinspection unchecked
        ((Map<String, Object>) websiteSource.getMetadata().getOrDefault(MetadataKeys.ADDITIONAL_PROPERTIES.getValue(), Map.of()))
                .forEach((key, value) -> {
                    if (!MetadataKeys.TITLE.getValue().equals(key))
                        dto.putAdditionalProperty(key, value);
                });
        dto.putAdditionalProperty("embedded", embedded);
        return dto;
    }

}
