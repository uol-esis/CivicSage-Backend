package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.FeedbackApiDelegate;
import de.uol.pgdoener.civicsage.business.dto.FeedbackDto;
import de.uol.pgdoener.civicsage.business.feedback.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * More or less exact copy of FeedbackController implemented in TH1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApiDelegate {

    private final FeedbackService feedbackService;

    @Override
    public ResponseEntity<UUID> submitFeedback(FeedbackDto request) {
        log.debug("Received Feedback {}", request);
        UUID id = feedbackService.create(request);
        log.debug("Feedback saved with id {}", id);
        return ResponseEntity.status(201).body(id);
    }

}
