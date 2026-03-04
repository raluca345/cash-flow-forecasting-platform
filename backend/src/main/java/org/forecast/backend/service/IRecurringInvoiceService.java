package org.forecast.backend.service;

import org.springframework.transaction.annotation.Transactional;

public interface IRecurringInvoiceService {
    @Transactional
    void generateDueInvoices();
}
