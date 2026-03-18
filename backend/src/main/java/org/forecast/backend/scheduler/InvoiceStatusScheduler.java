package org.forecast.backend.scheduler;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.service.InvoiceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceStatusScheduler {

    private final InvoiceService invoiceService;

    @Scheduled(cron = "0 30 2 * * ?")
    public void updateOverdueInvoices() {
        invoiceService.markOverdueInvoicesForAllCompanies();
    }
}
