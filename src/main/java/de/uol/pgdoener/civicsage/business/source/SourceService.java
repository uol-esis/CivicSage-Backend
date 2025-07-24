package de.uol.pgdoener.civicsage.business.source;

import de.uol.pgdoener.civicsage.business.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.business.source.exception.SourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {

    private final FileSourceRepository fileSourceRepository;
    private final WebsiteSourceRepository websiteSourceRepository;

    public FileSource save(FileSource fileSource) {
        return fileSourceRepository.save(fileSource);
    }

    public WebsiteSource save(WebsiteSource websiteSource) {
        return websiteSourceRepository.save(websiteSource);
    }

    public FileSource getFileSourceById(UUID id) {
        Optional<FileSource> optionalFileSource = fileSourceRepository.findById(id);
        if (optionalFileSource.isEmpty())
            throw new SourceNotFoundException("Could not find source with id +" + id);
        return optionalFileSource.get();
    }

    public Optional<FileSource> getFileSourceByHash(String hash) {
        return fileSourceRepository.getFileSourceByHash(hash);
    }

    public Optional<WebsiteSource> getWebsiteSourceByUrl(String url) {
        return websiteSourceRepository.findByUrl(url);
    }

    public void verifyFileHashNotIndexed(String hash) {
        if (fileSourceRepository.existsByHash(hash)) {
            throw new SourceCollisionException("File is already indexed");
        }
    }

    public List<FileSource> getFileSourcesNotIndexedWith(String modelId) {
        return fileSourceRepository.getFileSourceByModelsNotContaining(modelId);
    }

    public List<WebsiteSource> getWebsiteSourcesNotIndexedWith(String modelId) {
        return websiteSourceRepository.getWebsiteSourceByModelsNotContaining(modelId);
    }

    public Iterable<FileSource> getAllFileSources(String filterExpression) {
        // TODO: Implement filtering logic based on the filterExpression
        return fileSourceRepository.findAll();
    }

    public Iterable<WebsiteSource> getAllWebsiteSources(String filterExpression) {
        // TODO: Implement filtering logic based on the filterExpression
        return websiteSourceRepository.findAll();
    }

    public void deleteSource(UUID id) {
        log.info("Deleting source with id: {}", id);
        fileSourceRepository.deleteById(id);
        websiteSourceRepository.deleteById(id);
    }

    public boolean existsById(UUID id) {
        return fileSourceRepository.existsById(id) || websiteSourceRepository.existsById(id);
    }
}
