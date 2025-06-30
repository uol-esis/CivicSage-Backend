package de.uol.pgdoener.civicsage.source;

import de.uol.pgdoener.civicsage.source.exception.SourceCollisionException;
import de.uol.pgdoener.civicsage.source.exception.SourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {

    private final FileSourceRepository fileSourceRepository;
    private final WebsiteSourceRepository websiteSourceRepository;
    private final FileHashingService fileHashingService;

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

    public Optional<WebsiteSource> getWebsiteSourceByUrl(String url) {
        return websiteSourceRepository.findByUrl(url);
    }

    public void verifyWebsiteNotIndexed(String url) {
        if (websiteSourceRepository.existsByUrl(url)) {
            throw new SourceCollisionException("Website is already indexed");
        }
    }

    public void verifyFileHashNotIndexed(String hash) {
        if (fileSourceRepository.existsByHash(hash)) {
            throw new SourceCollisionException("File is already indexed");
        }
    }

}
