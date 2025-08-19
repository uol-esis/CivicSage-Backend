package de.uol.pgdoener.civicsage.business.completion;

import lombok.Getter;

@Getter
public class WebsiteMedia extends CustomMedia {

    private final String url;

    protected WebsiteMedia(String url) {
        this.url = url;
    }

}
