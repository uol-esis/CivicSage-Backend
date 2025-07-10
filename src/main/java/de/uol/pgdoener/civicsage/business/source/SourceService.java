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

    public void save(FileSource fileSource) {
        fileSourceRepository.save(fileSource);
    }

    public void save(WebsiteSource websiteSource) {
        websiteSourceRepository.save(websiteSource);
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

}
