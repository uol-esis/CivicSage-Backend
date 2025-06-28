package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "civicsage.ai")
public class AIProperties {

    private Chat chat;
    private Embedding embedding;

    @Data
    public static class Chat {

        private Model model;

        @Data
        public static class Model {
            /**
             * The context length for the chat model.
             */
            private Integer contextLength = 8000;
        }
    }

    @Data
    public static class Embedding {

        private Model model;

        @Data
        public static class Model {
            /**
             * The context length for the embedding model.
             */
            private Integer contextLength = 256;
        }
    }

}
