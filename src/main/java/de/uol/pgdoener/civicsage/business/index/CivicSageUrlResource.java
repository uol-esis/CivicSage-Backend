package de.uol.pgdoener.civicsage.business.index;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;

/**
 * Custom UrlResource that sets a specific User-Agent header for HTTP requests.
 */
public class CivicSageUrlResource extends UrlResource {

    public CivicSageUrlResource(URI uri) throws MalformedURLException {
        super(uri);
    }

    public CivicSageUrlResource(String path) throws MalformedURLException {
        super(path);
    }

    @Override
    protected void customizeConnection(@NotNull URLConnection con) throws IOException {
        super.customizeConnection(con);
        con.addRequestProperty("User-Agent", "CivicSage Document Reader");
    }

}
