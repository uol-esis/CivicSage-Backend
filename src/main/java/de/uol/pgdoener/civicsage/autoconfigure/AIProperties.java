package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "civicsage.ai")
public class AIProperties {

    private Chat chat = new Chat();
    private Embedding embedding = new Embedding();

    @Data
    public static class Chat {

        private Model model = new Model();

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

        private Model model = new Model();

        @Data
        public static class Model {
            /**
             * The context length for the embedding model.
             */
            private Integer contextLength = 256;
        }
    }

}
