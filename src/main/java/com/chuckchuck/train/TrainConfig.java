package com.chuckchuck.train;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrainConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
