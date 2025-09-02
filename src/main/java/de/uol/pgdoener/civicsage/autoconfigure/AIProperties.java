package de.uol.pgdoener.civicsage.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "civicsage.ai")
public class AIProperties {

    private Chat chat = new Chat();
    private Embedding embedding = new Embedding();
    private VectorStore vectorStore = new VectorStore();

    @Data
    public static class Chat {

        private Model model = new Model();
        private int maxEmbeddings = 10;

        private String defaultSystemPrompt = """
                Beantworte die folgende Frage ausschließlich basierend auf den bereitgestellten Dokumenten. Nutze keine externen Informationen.
                
                Antwort:
                - Wenn die Antwort vollständig im Dokument enthalten ist: Formuliere sie klar und korrekt.
                - Wenn keine ausreichenden Informationen vorhanden sind: Sage "Dazu liegen keine Informationen vor."
                """;

        /**
         * If a chat has not had any interaction after this duration, it will be deleted.
         * Default is 4 days.
         */
        private Duration chatLifetime = Duration.ofDays(4);

        /**
         * If an uploaded file has not been used in any chat after this duration, it will be deleted.
         * Default is 1 hour.
         */
        private Duration unusedFileLifetime = Duration.ofHours(1);

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
        /**
         * The delay before retrying an embedding task if it fails with Spring AI's retry mechanism.
         */
        private Duration retryDelay = Duration.ofMinutes(1);

        /**
         * The maximum length of documents to embed.
         * Must be less than or equal to the context length of the model.
         */
        private int documentContextLength = 256;

        @Data
        public static class Model {
            /**
             * The context length for the embedding model.
             */
            private Integer contextLength = 256;
        }
    }

    @Data
    public static class VectorStore {
        /**
         * The type of the vector store.
         */
        private Type type = Type.MARIADB;
        /**
         * Whether to initialize the required schema
         */
        private boolean initializeSchema = false;
        /**
         * Whether to remove the existing vector store table if it exists.
         */
        private boolean removeExistingVectorStoreTable = false;
        /**
         * The name of the schema to use for the vector store.
         * This is used to create a unique table name based on the model name.
         */
        private String schemaName = null;
        /**
         * Enables schema and table name validation to ensure they are valid and existing objects.
         */
        private boolean schemaValidation = false;
        /**
         * The prefix for the table names in the vector store.
         * This is used to create a unique table name based on the model name.
         */
        private String tableNamePrefix = "vector_store_";
        /**
         * The distance metric to use for the vector store.
         * See options for more details.
         */
        private Distance distance = Distance.COSINE;

        public enum Type {
            MARIADB, POSTGRESQL
        }

        public enum Distance {
            /**
             * Cosine distance metric.
             * <p>
             * MariaDB: {@link org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore.MariaDBDistanceType#COSINE}
             * PostgreSQL: {@link org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType#COSINE_DISTANCE}
             */
            COSINE,
            /**
             * Euclidean distance metric.
             * <p>
             * MariaDB: {@link org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore.MariaDBDistanceType#EUCLIDEAN}
             * PostgreSQL: {@link org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType#EUCLIDEAN_DISTANCE}
             */
            EUCLIDEAN,
            /**
             * Inner product distance metric.
             * <p>
             * MariaDB: N/A
             * PostgreSQL: {@link org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType#NEGATIVE_INNER_PRODUCT}
             */
            NEGATIVE_INNER_PRODUCT
        }
    }

}
