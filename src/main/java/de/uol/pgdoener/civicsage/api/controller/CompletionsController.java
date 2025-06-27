package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.CompletionsApi;
import de.uol.pgdoener.civicsage.business.dto.SummarizeEmbeddings200ResponseDto;
import de.uol.pgdoener.civicsage.business.dto.SummarizeEmbeddingsRequestDto;
import de.uol.pgdoener.civicsage.completion.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CompletionsController implements CompletionsApi {

    private final SummaryService summaryService;

    @Override
    public ResponseEntity<SummarizeEmbeddings200ResponseDto> summarizeEmbeddings(SummarizeEmbeddingsRequestDto summarizeEmbeddingsRequestDto) {
        log.info("Generating summary");
        String summary = summaryService.summarize(
                summarizeEmbeddingsRequestDto.getDocumentIds(),
                summarizeEmbeddingsRequestDto.getPrompt(),
                summarizeEmbeddingsRequestDto.getSystemPrompt()
        );
        log.info("Successfully generated summary");

        SummarizeEmbeddings200ResponseDto response = new SummarizeEmbeddings200ResponseDto()
                .summary(summary);
        return ResponseEntity.ok(response);
    }

}
