package de.uol.pgdoener.civicsage.business.embedding;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class PgVectorStoreExtension /*implements VectorStoreExtension*/ {

//    private final JdbcTemplate template;
//
//    @Value("${spring.ai.vectorstore.pgvector.table-name}")
//    private String tableName;
//
//    public PgVectorStoreExtension(PgVectorStore pgVectorStore) {
//        Optional<JdbcTemplate> optTemplate = pgVectorStore.getNativeClient();
//        template = optTemplate.orElseThrow(() -> new RuntimeException("Could not get native client from PgVectorStore"));
//    }
//
//    //https://www.baeldung.com/spring-jdbctemplate-in-list
//    @Override
//    public List<Document> getById(List<UUID> documentIds) {
//        String sql = buildSQL(documentIds.size());
//        log.debug("Created SQL statement: {}", sql);
//        List<Document> documents = template.query(
//                sql,
//                (rs, rowNum) -> Document.builder()
//                        .id(rs.getObject("id").toString())
//                        .text(rs.getString("content"))
//                        .build(),
//                documentIds.toArray()
//        );
//        log.debug("Retrieved {} documents", documents.size());
//
//        if (documents.size() != documentIds.size())
//            throw new DocumentNotFoundException("Could not find all requested documents");
//        return documents;
//    }
//
//    private String buildSQL(int idsCount) {
//        String idPlaceHolders = String.join(",", Collections.nCopies(idsCount, "?"));
//
//        return "SELECT id, content FROM " + tableName + " WHERE id IN (" + idPlaceHolders + ")";
//    }

}
