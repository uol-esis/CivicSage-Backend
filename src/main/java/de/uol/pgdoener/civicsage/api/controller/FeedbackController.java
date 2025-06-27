package de.uol.pgdoener.civicsage.api.controller;

import de.uol.pgdoener.civicsage.api.FeedbackApi;
import de.uol.pgdoener.civicsage.business.dto.FeedbackDto;
import de.uol.pgdoener.civicsage.feedback.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * More or less exact copy of FeedbackController implemented in TH1.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApi {

    private final FeedbackService feedbackService;

    @Override
    public ResponseEntity<UUID> submitFeedback(FeedbackDto request) {
        log.debug("Received Feedback {}", request);
        UUID id = feedbackService.create(request);
        log.debug("Feedback saved with id {}", id);
        return ResponseEntity.status(201).body(id);
    }

}
