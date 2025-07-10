package de.uol.pgdoener.civicsage.business.feedback;

import de.uol.pgdoener.civicsage.business.dto.FeedbackDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FeedbackMapper {

    public static Feedback toEntity(FeedbackDto dto) {
        return new Feedback(
                null,
                dto.getContent()
        );
    }

}
