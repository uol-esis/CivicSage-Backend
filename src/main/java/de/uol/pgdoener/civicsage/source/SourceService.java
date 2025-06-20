package de.uol.pgdoener.civicsage.source;

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

    public void save(FileSource fileSource) {
        fileSourceRepository.save(fileSource);
    }

    public FileSource getFileSourceById(UUID id) {
        Optional<FileSource> optionalFileSource = fileSourceRepository.findById(id);
        if (optionalFileSource.isEmpty())
            throw new SourceNotFoundException("Could not find source with id +" + id);
        return optionalFileSource.get();
    }

}
