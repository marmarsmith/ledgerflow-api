
package com.marcel.ledgerflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public Clock applicationClock(
            @Value("${ledgerflow.recurring.zone:UTC}") String zone
    ) {
        return Clock.system(ZoneId.of(zone));
    }
}
