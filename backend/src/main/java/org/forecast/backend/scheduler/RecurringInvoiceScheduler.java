package org.forecast.backend.scheduler;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.service.RecurringInvoiceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringInvoiceScheduler {

    private final RecurringInvoiceService recurringInvoiceService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyGeneration() {
        recurringInvoiceService.generateDueInvoices();
    }
}
