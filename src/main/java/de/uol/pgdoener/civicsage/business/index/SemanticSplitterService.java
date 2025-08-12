package de.uol.pgdoener.civicsage.business.index;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticSplitterService {

    public List<Document> process(@NonNull List<Document> documents) {
        // TODO split chunks into semantically meaningful chunks
        return documents;
    }

}
