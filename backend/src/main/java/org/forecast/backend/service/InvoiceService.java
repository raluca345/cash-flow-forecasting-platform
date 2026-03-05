package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forecast.backend.dtos.CreateInvoiceRequest;
import org.forecast.backend.dtos.InvoiceSearchCriteria;
import org.forecast.backend.dtos.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.forecast.backend.specifications.InvoiceSpecifications.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService implements IInvoiceService{

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final ClientService clientService;
    private final ExchangeRateService exchangeRateService;

    @Value("${app.currency.base:USD}")
    private String baseCurrency;

    @Override
    public Invoice createInvoice(CreateInvoiceRequest request) {

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());

        Client client = clientService.get(request.getClientId());

        invoice.setClient(client);
        invoice.setAmount(request.getAmount());
        invoice.setCurrency(request.getCurrency().toUpperCase());

        BigDecimal rateBaseToInvoice = exchangeRateService.getRate(baseCurrency, invoice.getCurrency());
        if (rateBaseToInvoice == null || rateBaseToInvoice.signum() <= 0) {
            throw new IllegalStateException("Derived exchange rate must be > 0");
        }

        invoice.setExchangeRate(rateBaseToInvoice);

        // Frankfurter semantics: 1 baseCurrency = rate * invoiceCurrency
        // So: amountBaseCurrency = amountInvoice / rate
        BigDecimal amountBase = request.getAmount()
                .divide(rateBaseToInvoice, 2, RoundingMode.HALF_UP);
        invoice.setAmountBaseCurrency(amountBase);

        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setDeleted(false);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice getInvoice(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumberAndDeletedFalse(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No invoice with the number " + invoiceNumber + " " +
                        "found."));
    }

    @Override
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findByDeletedFalseOrderByIssueDateDesc(pageable);
    }

    @Override
    public List<Invoice> getOpenInvoicesByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        return invoiceRepository.findByDeletedFalse(pageable).toList();
    }

    @Override
    public List<Invoice> getOpenInvoicesByStatus(InvoiceStatus status) {
        Pageable pageable = PageRequest.of(0, 10);
        return invoiceRepository.findByStatusAndDeletedFalse(status, pageable);
    }

    @Override
    public Invoice sendInvoice(String invoiceNumber) {
        Invoice invoice = getInvoice(invoiceNumber);
        invoice.markAsSent();
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice payInvoice(String invoiceNumber) {
        Invoice invoice = getInvoice(invoiceNumber);
        invoice.markAsPaid(Instant.now());
        return invoiceRepository.save(invoice);
    }

    @Override
    public void deleteInvoice(String invoiceNumber) {
        Invoice invoice = getInvoice(invoiceNumber);
        if (invoice.isDraft()) {
            invoice.setDeleted(true);
            invoiceRepository.save(invoice);
        }
        else {
            throw new UnsupportedOperationException("Only drafts can be deleted.");
        }
    }

    @Override
    public Invoice cancelInvoice(String invoiceNumber) {
        Invoice invoice = getInvoice(invoiceNumber);
        if (invoice.isCancelled()) {
            throw new UnsupportedOperationException("Invoice is already cancelled.");
        }
        invoice.cancel();
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice editInvoice(String invoiceNumber, UpdateInvoiceDraftPartialRequest request) {
        Invoice invoice = getInvoice(invoiceNumber);
        if (!invoice.isDraft()) {
            throw new UnsupportedOperationException("Only drafts can be edited.");
        }

        if (request.getAmount() != null) {
            invoice.setAmount(request.getAmount());

            BigDecimal baseAmount = request.getAmount()
                    .divide(invoice.getExchangeRate(), 2, RoundingMode.HALF_UP);
            invoice.setAmountBaseCurrency(baseAmount);
        }
        if (request.getIssueDate() != null) {
            invoice.setIssueDate(request.getIssueDate());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        return invoiceRepository.save(invoice);
    }

    @Override
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<InvoiceStatus> eligibleStatuses = List.of(InvoiceStatus.SENT);

        int pageSize = 100;
        int pageNumber = 0;
        int totalProcessed = 0;

        Page<Invoice> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = invoiceRepository.findByStatusInAndDueDateBeforeAndDeletedFalse(
                    eligibleStatuses,
                    today,
                    pageable
            );

            List<Invoice> overdueInvoices = page.getContent();

            for (Invoice invoice : overdueInvoices) {
                invoice.markOverdue(today);
                totalProcessed++;
            }

            invoiceRepository.saveAll(overdueInvoices);
            pageNumber++;
            log.info("Processed page {} of overdue invoices. Count: {}", pageNumber, overdueInvoices.size());

        } while (page.hasNext());

        log.info("Completed marking overdue invoices. Total processed: {}", totalProcessed);
    }

    @Override
    public Page<Invoice> filterByCriteria(InvoiceSearchCriteria criteria, Pageable pageable) {
        Specification<Invoice> spec = Specification
                .where(notDeleted())
                .and(hasStatus(criteria.getStatus()))
                .and(hasClientId(criteria.getClientId()))
                .and(clientNameContains(criteria.getClientName()))
                .and(invoiceNumberContains(criteria.getInvoiceNumber()))
                .and(hasCurrency(criteria.getCurrency()))
                .and(amountGreaterThan(criteria.getMinAmount()))
                .and(amountLessThan(criteria.getMaxAmount()))
                .and(dueDateFrom(criteria.getDueDateFrom()))
                .and(dueDateTo(criteria.getDueDateTo()))
                .and(issueDateFrom(criteria.getIssueDateFrom()))
                .and(issueDateTo(criteria.getIssueDateTo()))
                .and(isOverdue(criteria.getOverdue()))
                .and(isUnpaid(criteria.getUnpaid()))
                .and(sentAtFrom(criteria.getSentAtFrom()))
                .and(sentAtTo(criteria.getSentAtTo()))
                .and(paidAtFrom(criteria.getPaidAtFrom()))
                .and(paidAtTo(criteria.getPaidAtTo()));

        return invoiceRepository.findAll(spec, pageable);
    }
}
