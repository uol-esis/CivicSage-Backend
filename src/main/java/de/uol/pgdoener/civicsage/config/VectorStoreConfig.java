package de.uol.pgdoener.civicsage.config;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final AIProperties aiProperties;
    private final VectorStoreTableNameProvider vectorStoreTableNameProvider;
    private final EmbeddingModel embeddingModel;

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelName;

    @Bean
    public VectorStore vectorStore() {
        return switch (aiProperties.getVectorStore().getType()) {
            case MARIADB -> createMariaDBVectorStore();
            case POSTGRESQL -> createPgVectorStore();
        };
    }

    // #####################
    // MariaDB Vector Store
    // #####################

    private MariaDBVectorStore createMariaDBVectorStore() {
        return MariaDBVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(aiProperties.getVectorStore().isInitializeSchema())
                .removeExistingVectorStoreTable(aiProperties.getVectorStore().isRemoveExistingVectorStoreTable())
                .schemaName(aiProperties.getVectorStore().getSchemaName())
                .schemaValidation(aiProperties.getVectorStore().isSchemaValidation())
                .vectorTableName(vectorStoreTableNameProvider.getTableName())
                .distanceType(createMariaDBDistanceType())
                .build();
    }

    private MariaDBVectorStore.MariaDBDistanceType createMariaDBDistanceType() {
        return switch (aiProperties.getVectorStore().getDistance()) {
            case COSINE -> MariaDBVectorStore.MariaDBDistanceType.COSINE;
            case EUCLIDEAN -> MariaDBVectorStore.MariaDBDistanceType.EUCLIDEAN;
            case NEGATIVE_INNER_PRODUCT -> throw new IllegalStateException(
                    "Negative inner product distance is not supported by MariaDBVectorStore."
            );
        };
    }

    // ########################
    // PostgreSQL Vector Store
    // ########################

    private PgVectorStore createPgVectorStore() {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(aiProperties.getVectorStore().isInitializeSchema())
                .removeExistingVectorStoreTable(aiProperties.getVectorStore().isRemoveExistingVectorStoreTable())
                .schemaName(aiProperties.getVectorStore().getSchemaName())
                .vectorTableValidationsEnabled(aiProperties.getVectorStore().isSchemaValidation())
                .vectorTableName(vectorStoreTableNameProvider.getTableName())
                .distanceType(createPgDistanceType())
                .build();
    }

    private PgVectorStore.PgDistanceType createPgDistanceType() {
        return switch (aiProperties.getVectorStore().getDistance()) {
            case COSINE -> PgVectorStore.PgDistanceType.COSINE_DISTANCE;
            case EUCLIDEAN -> PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE;
            case NEGATIVE_INNER_PRODUCT -> PgVectorStore.PgDistanceType.NEGATIVE_INNER_PRODUCT;
        };
    }

}
