package org.forecast.backend.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forecast.backend.service.FrankfurterExchangeRateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final FrankfurterExchangeRateService frankfurterExchangeRateService;

    @PostConstruct
    public void refreshOnStartup() {
        refreshLatestRates();
    }

    /**
     * Fetch and upsert latest exchange rates once per day at midnight server time.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshLatestRates() {
        int upserted = frankfurterExchangeRateService.fetchAndUpsertLatestBaseRates();
        log.info("Refreshed FX rates from Frankfurter. Upserted {} rows", upserted);
    }
}
