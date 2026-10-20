
package com.marcel.ledgerflow.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigTest {

    private final ClockConfig clockConfig = new ClockConfig();

    @Test
    void applicationClock_usesUtcTimezone() {

        Clock clock = clockConfig.applicationClock("UTC");

        assertEquals(
                ZoneId.of("UTC"),
                clock.getZone()
        );
    }


    @Test
    void applicationClock_usesConfiguredTimezone() {

        Clock clock = clockConfig.applicationClock("America/Chicago");

        assertEquals(
                ZoneId.of("America/Chicago"),
                clock.getZone()
        );
    }
}
