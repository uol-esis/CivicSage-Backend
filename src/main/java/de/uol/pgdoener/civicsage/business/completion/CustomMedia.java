package de.uol.pgdoener.civicsage.business.completion;

import lombok.Getter;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;

/**
 * This is a custom media class that extends the Media class from Spring AI.
 * It is used to provide media in the user message for LLMs that do not support media natively.
 * The content is added to the user message by the {@link MediaConversionAdvisor}.
 */
@Getter
public abstract class CustomMedia extends Media {

    protected CustomMedia() {
        super(Format.DOC_TXT, new ByteArrayResource(new byte[0])); // Setting dummy data
    }

}
