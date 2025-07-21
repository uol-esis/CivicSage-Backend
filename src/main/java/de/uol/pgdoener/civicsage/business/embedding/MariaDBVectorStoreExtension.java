package de.uol.pgdoener.civicsage.business.embedding;

import de.uol.pgdoener.civicsage.business.embedding.exception.DocumentNotFoundException;
import de.uol.pgdoener.civicsage.config.VectorStoreTableNameProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "civicsage.ai.vectorstore.type", havingValue = "MARIADB")
public class MariaDBVectorStoreExtension implements VectorStoreExtension {

    private final JdbcTemplate template;
    private final String tableName;

    public MariaDBVectorStoreExtension(VectorStore mariaDBVectorStore, VectorStoreTableNameProvider tableNameProvider) {
        this.tableName = tableNameProvider.getTableName();
        Optional<JdbcTemplate> optTemplate = mariaDBVectorStore.getNativeClient();
        template = optTemplate.orElseThrow(() -> new RuntimeException("Could not get native client from MariaDBVectorStore"));
    }

    //https://www.baeldung.com/spring-jdbctemplate-in-list
    @Override
    public List<Document> getById(List<UUID> documentIds) {
        String sql = buildSQL(documentIds.size());
        log.debug("Created SQL statement: {}", sql);
        List<Document> documents = template.query(
                sql,
                (rs, rowNum) -> Document.builder()
                        .id(rs.getObject("id").toString())
                        .text(rs.getString("content"))
                        .build(),
                documentIds.toArray()
        );
        log.debug("Retrieved {} documents", documents.size());

        if (documents.size() != documentIds.size())
            throw new DocumentNotFoundException("Could not find all requested documents");
        return documents;
    }

    private String buildSQL(int idsCount) {
        String idPlaceHolders = String.join(",", Collections.nCopies(idsCount, "?"));

        return "SELECT id, content FROM " + tableName + " WHERE id IN (" + idPlaceHolders + ")";
    }

}
