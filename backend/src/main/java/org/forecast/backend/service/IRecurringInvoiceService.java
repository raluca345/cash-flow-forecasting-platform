package org.forecast.backend.service;

import java.util.UUID;
import org.forecast.backend.dtos.recurringinvoice.CreateRecurringInvoiceRequest;
import org.forecast.backend.dtos.recurringinvoice.UpdateRecurringInvoiceRequest;
import org.forecast.backend.model.RecurringInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface IRecurringInvoiceService {
    RecurringInvoice create(CreateRecurringInvoiceRequest request);

    RecurringInvoice get(UUID recurringInvoiceId);

    Page<RecurringInvoice> listAll(Pageable pageable);

    RecurringInvoice update(UUID recurringInvoiceId, UpdateRecurringInvoiceRequest request);

    RecurringInvoice activate(UUID recurringInvoiceId);

    RecurringInvoice deactivate(UUID recurringInvoiceId);

    @Transactional
    void generateDueInvoices();
}
