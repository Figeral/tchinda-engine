package com.ubuntux.tchinda.scheduler;

import com.ubuntux.tchinda.engine.IntelligenceEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyJob {

    private final IntelligenceEngine intelligenceEngine;

    // Run every day at 8:00 AM (server local time)
    // Or we could use fixedDelay/fixedRate for testing.
    // Run every minute for testing purposes
    @Scheduled(cron = "${app.scheduler.cron:0 * * * * ?}")
    public void runRadar() {
        log.info("Triggering scheduled event radar job");
        try {
            intelligenceEngine.runDailyCycle();
        } catch (Exception e) {
            log.error("Job failure: ", e);
        }
    }
}
