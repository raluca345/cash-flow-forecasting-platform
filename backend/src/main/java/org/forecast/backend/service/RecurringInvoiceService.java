package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.model.RecurringInvoice;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.repository.RecurringInvoiceRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringInvoiceService implements IRecurringInvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final RecurringInvoiceRepository recurringInvoiceRepository;
    private final InvoiceNumberGenerator generator;

    @Transactional
    @Override
    public void generateDueInvoices() {
        LocalDate today = LocalDate.now();

        List<RecurringInvoice> dueRecurring =
                recurringInvoiceRepository.findDueWithLock(today);

        dueRecurring.forEach(recurringInvoice -> {
            Invoice invoice = recurringInvoice.generateInvoice(generator.generateInvoiceNumber());
            invoiceRepository.save(invoice);

            recurringInvoice.advanceNextGenerationDate();
        });
    }
}
