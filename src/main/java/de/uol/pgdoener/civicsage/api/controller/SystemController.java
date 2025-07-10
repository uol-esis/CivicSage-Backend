package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.SystemApiDelegate;
import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.business.dto.SystemInfoDto;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemController implements SystemApiDelegate {

    private final OpenAPI openAPI;
    private final AIProperties aiProperties;
    private final MultipartProperties multipartProperties;

    @Override
    public ResponseEntity<SystemInfoDto> getSystemInfo() {

        SystemInfoDto systemInfoDto = new SystemInfoDto();
        systemInfoDto.setServerVersion(getClass().getPackage().getImplementationVersion());
        systemInfoDto.setApiVersion(openAPI.getInfo().getVersion());

        systemInfoDto.setChatContextWindow(aiProperties.getChat().getModel().getContextLength());
        systemInfoDto.setEmbeddingContextWindow(aiProperties.getEmbedding().getModel().getContextLength());
        systemInfoDto.setMaxEmbeddingsInChat(aiProperties.getChat().getMaxEmbeddings());

        systemInfoDto.setMaxFileSize((int) multipartProperties.getMaxFileSize().toMegabytes());

        return ResponseEntity.ok(systemInfoDto);
    }
}
