package com.coldchain.triage.shared.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    private static final Duration POSTGRES_RESOLUTION = Duration.ofNanos(1_000);

    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), POSTGRES_RESOLUTION);
    }
}
