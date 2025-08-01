package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.autoconfigure.AIProperties;
import de.uol.pgdoener.civicsage.config.VectorStoreTableNameProvider;
import io.micrometer.common.lang.NonNullApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnusedModelsVectorStores {

    private final DataSource dataSource;
    private final VectorStoreTableNameProvider vectorStoreTableNameProvider;
    private final AIProperties aiProperties;

    private final JdbcTemplate jdbcTemplate;

    private final List<VectorStore> vectorStores = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<TableLocation> vectorStoreTables = getVectorStoreTables();
        // remove the vector store table for the current model
        vectorStoreTables.removeIf(l ->
                l.tableName.equals(vectorStoreTableNameProvider.getTableName()) &&
                        Objects.equals(l.schemaName, aiProperties.getVectorStore().getSchemaName())
        );
        log.info("VectorStore tables for unused models: {}", vectorStoreTables);

        for (TableLocation tableLocation : vectorStoreTables) {
            VectorStore vectorStore = createVectorStore(tableLocation);
            vectorStores.add(vectorStore);
        }
    }

    public void delete(Filter.Expression expression) {
        for (VectorStore vectorStore : vectorStores) {
            try {
                vectorStore.delete(expression);
            } catch (Exception e) {
                log.error("Failed to delete embeddings from vector store: {}", vectorStore, e);
            }
        }
    }

    private List<TableLocation> getVectorStoreTables() {
        List<TableLocation> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String[] types = {"TABLE"};

            try (ResultSet rs = metaData.getTables(null, null, aiProperties.getVectorStore().getTableNamePrefix() + "%", types)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String schemaName = rs.getString("TABLE_SCHEM");
                    tables.add(new TableLocation(schemaName, tableName));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to retrieve table names of vector stores. Embeddings will not be deleted from them, if a source is deleted!", e);
        }
        return tables;
    }

    private VectorStore createVectorStore(TableLocation tableLocation) {
        return switch (aiProperties.getVectorStore().getType()) {
            case MARIADB -> MariaDBVectorStore.builder(jdbcTemplate, new NoOpEmbeddingModel())
                    .initializeSchema(false)
                    .removeExistingVectorStoreTable(false)
                    .schemaName(tableLocation.schemaName())
                    .schemaValidation(false)
                    .vectorTableName(tableLocation.tableName())
                    .build();
            case POSTGRESQL -> PgVectorStore.builder(jdbcTemplate, new NoOpEmbeddingModel())
                    .initializeSchema(false)
                    .removeExistingVectorStoreTable(false)
                    .schemaName(tableLocation.schemaName())
                    .vectorTableValidationsEnabled(false)
                    .vectorTableName(tableLocation.tableName())
                    .build();
        };
    }

    private record TableLocation(String schemaName, String tableName) {
    }

    @NonNullApi
    private static final class NoOpEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float[] embed(Document document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int dimensions() {
            return 1;
        }
    }

}
