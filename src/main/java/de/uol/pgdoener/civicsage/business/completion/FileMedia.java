package de.uol.pgdoener.civicsage.business.completion;

import de.uol.pgdoener.civicsage.business.source.FileSource;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
public class FileMedia extends CustomMedia {

    private final FileSource fileSource;
    private final Resource resource;

    protected FileMedia(Resource resource, FileSource fileSource) {
        this.resource = resource;
        this.fileSource = fileSource;
    }

}
