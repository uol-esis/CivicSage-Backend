package de.uol.pgdoener.civicsage.business.index;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Tardis is a utility class that provides the current time in the system's default time zone.
 * It is used to get the current time for various operations, such as storing file metadata.
 */
@Component
public class TimeFactory {

    public OffsetDateTime getCurrentTime() {
        return OffsetDateTime.now(ZoneId.systemDefault());
    }

}
