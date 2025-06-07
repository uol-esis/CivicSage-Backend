package de.uol.pgdoener.civicsage.mapper;


import de.uol.pgdoener.civicsage.business.dto.SearchResultDto;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.FILE_NAME;
import static de.uol.pgdoener.civicsage.index.document.MetadataKeys.URL;
import static org.junit.jupiter.api.Assertions.*;

class SearchResultMapperTest {

    SearchResultMapper searchResultMapper = new SearchResultMapper();

    @Test
    void testToDtoFile() {
        Document document = Document.builder()
                .text("content")
                .metadata(Map.of(
                        FILE_NAME, "file.pdf"
                ))
                .score(0.42)
                .build();

        SearchResultDto searchResultDto = searchResultMapper.toDto(document);

        assertEquals("content", searchResultDto.getText());
        assertTrue(searchResultDto.getFileName().isPresent());
        assertEquals("file.pdf", searchResultDto.getFileName().get());
        assertTrue(searchResultDto.getFileRef().isPresent());
        assertEquals("TODO", searchResultDto.getFileRef().get());
        assertTrue(searchResultDto.getUrl().isEmpty());
        assertEquals(0.42, searchResultDto.getScore());
    }

    @Test
    void testToDtoUrl() {
        Document document = Document.builder()
                .text("content")
                .metadata(Map.of(
                        URL, "example.com"
                ))
                .score(0.1)
                .build();

        SearchResultDto searchResultDto = searchResultMapper.toDto(document);

        assertEquals("content", searchResultDto.getText());
        assertTrue(searchResultDto.getFileName().isEmpty());
        assertTrue(searchResultDto.getFileRef().isEmpty());
        assertTrue(searchResultDto.getUrl().isPresent());
        assertEquals("example.com", searchResultDto.getUrl().get());
        assertEquals(0.1, searchResultDto.getScore());
    }

    @Test
    void testToDtoList() {
        List<Document> documents = List.of(
                Document.builder()
                        .text("content1")
                        .metadata(Map.of(
                                FILE_NAME, "file.pdf"
                        ))
                        .score(0.42)
                        .build(),
                Document.builder()
                        .text("content2")
                        .metadata(Map.of(
                                URL, "example.com"
                        ))
                        .score(0.1)
                        .build()
        );

        List<SearchResultDto> searchResultDtos = searchResultMapper.toDto(documents);

        assertEquals(2, searchResultDtos.size());

        assertEquals("content1", searchResultDtos.getFirst().getText());
        assertTrue(searchResultDtos.getFirst().getFileName().isPresent());
        assertEquals("file.pdf", searchResultDtos.getFirst().getFileName().get());
        assertTrue(searchResultDtos.getFirst().getFileRef().isPresent());
        assertEquals("TODO", searchResultDtos.getFirst().getFileRef().get());
        assertTrue(searchResultDtos.getFirst().getUrl().isEmpty());
        assertEquals(0.42, searchResultDtos.getFirst().getScore());

        assertEquals("content2", searchResultDtos.getLast().getText());
        assertTrue(searchResultDtos.getLast().getFileName().isEmpty());
        assertTrue(searchResultDtos.getLast().getFileRef().isEmpty());
        assertTrue(searchResultDtos.getLast().getUrl().isPresent());
        assertEquals("example.com", searchResultDtos.getLast().getUrl().get());
        assertEquals(0.1, searchResultDtos.getLast().getScore());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testToDtoNull() {
        assertThrows(NullPointerException.class, () -> searchResultMapper.toDto((Document) null));
        assertThrows(NullPointerException.class, () -> searchResultMapper.toDto((List<Document>) null));
    }

}
