package de.uol.pgdoener.civicsage.business.feedback;

import de.uol.pgdoener.civicsage.business.dto.FeedbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public UUID create(FeedbackDto request) {
        Feedback feedback = FeedbackMapper.toEntity(request);
        return feedbackRepository.save(feedback).getId();
    }

}
