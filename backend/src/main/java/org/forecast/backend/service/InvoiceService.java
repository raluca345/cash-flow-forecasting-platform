package org.forecast.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forecast.backend.dtos.invoice.CreateInvoiceRequest;
import org.forecast.backend.dtos.invoice.InvoiceSearchCriteria;
import org.forecast.backend.dtos.invoice.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.repository.CompanyRepository;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.forecast.backend.util.LineItemFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
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
    private final CompanyRepository companyRepository;
    private final CompanySecurityService companySecurityService;

    @Value("${app.currency.base:USD}")
    private String baseCurrency;

    private void processOverdueInvoicesForCompany(UUID companyId, LocalDate today, List<InvoiceStatus> eligibleStatuses) {
        int pageSize = 100;
        int processedForCompany = 0;

        while (true) {
            // Always query page 0 because processed invoices no longer match the SENT filter after save.
            Pageable pageable = PageRequest.of(0, pageSize);
            Page<Invoice> page = invoiceRepository.findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(
                    companyId,
                    eligibleStatuses,
                    today,
                    pageable
            );

            List<Invoice> overdueInvoices = page.getContent();
            if (overdueInvoices.isEmpty()) {
                break;
            }

            for (Invoice invoice : overdueInvoices) {
                invoice.markOverdue(today);
                processedForCompany++;
            }

            invoiceRepository.saveAll(overdueInvoices);
            log.info("Processed {} overdue invoices for company {}", overdueInvoices.size(), companyId);
        }

        if (processedForCompany > 0) {
            log.info("Completed overdue processing for company {}. Total processed: {}", companyId, processedForCompany);
        }
    }

    @Override
    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());

        Company company = companyRepository.findById(currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + currentCompanyId + " found."));
        invoice.setCompany(company);

        Client client = resolveClient(request, currentCompanyId);
        invoice.setClient(client);
        invoice.setCurrency(request.getCurrency().toUpperCase());

        // Items-authoritative: map items (including per-item VAT)
        invoice.setItems(new ArrayList<>());
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                if (itemReq == null) continue;
                invoice.addItem(LineItemFactory.toInvoiceItem(itemReq));
            }
        }

        invoice.recalculateAmountFromItems();

        BigDecimal rateBaseToInvoice = exchangeRateService.getRate(baseCurrency, invoice.getCurrency());
        if (rateBaseToInvoice == null || rateBaseToInvoice.signum() <= 0) {
            throw new IllegalStateException("Derived exchange rate must be > 0");
        }

        invoice.setExchangeRate(rateBaseToInvoice);

        // Frankfurter semantics: 1 baseCurrency = rate * invoiceCurrency
        // So: amountBaseCurrency = totalAmount(invoice currency) / rate
        BigDecimal amountBase = invoice.getGrossTotal()
                .divide(rateBaseToInvoice, 2, RoundingMode.HALF_UP);
        invoice.setAmountBaseCurrency(amountBase);

        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice getInvoice(String invoiceNumber) {
        UUID currentCompany = companySecurityService.requireCurrentCompanyId("Company context required");
        return invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse(invoiceNumber, currentCompany)
                .orElseThrow(() -> new ResourceNotFoundException("No invoice with the number " + invoiceNumber + " found."));
    }

    @Transactional(readOnly = true)
    @Override
    public Invoice getInvoiceForPdf(String invoiceNumber) {
        UUID currentCompany = companySecurityService.requireCurrentCompanyId("Company context required");
        return invoiceRepository.findByInvoiceNumberForPdfAndCompanyId(invoiceNumber, currentCompany)
                .orElseThrow(() -> new ResourceNotFoundException("No invoice with the number " + invoiceNumber + " found."));
    }

    @Override
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        UUID currentCompany = companySecurityService.requireCurrentCompanyId("Company context required");
        return invoiceRepository.findByCompanyIdAndDeletedFalseOrderByIssueDateDesc(currentCompany, pageable);
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

        if (request.getItems() != null) {
            invoice.getItems().clear();
            for (var itemReq : request.getItems()) {
                if (itemReq == null) continue;
                invoice.addItem(LineItemFactory.toInvoiceItem(itemReq));
            }
        }

        // ensure totals are consistent if items changed
        if (request.getItems() != null) {
            invoice.recalculateAmountFromItems();

            BigDecimal baseAmount = invoice.getGrossTotal()
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
    public void markOverdueInvoicesForCompany(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company id is required");
        }
        LocalDate today = LocalDate.now();
        List<InvoiceStatus> eligibleStatuses = List.of(InvoiceStatus.SENT);
        processOverdueInvoicesForCompany(companyId, today, eligibleStatuses);
    }

    @Override
    public void markOverdueInvoicesForAllCompanies() {
        LocalDate today = LocalDate.now();
        List<InvoiceStatus> eligibleStatuses = List.of(InvoiceStatus.SENT);
        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            processOverdueInvoicesForCompany(company.getId(), today, eligibleStatuses);
        }
    }

    @Override
    public Page<Invoice> filterByCriteria(InvoiceSearchCriteria criteria, Pageable pageable) {
        criteria.validateRanges();
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");

        Specification<Invoice> spec = Specification
                .where(visibleToCompany(currentCompanyId))
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

    private Client resolveClient(CreateInvoiceRequest request, UUID currentCompanyId) {
        if (request.getClientId() != null && request.getNewClient() != null) {
            throw new IllegalArgumentException("Provide either clientId or newClient, not both.");
        }

        Client client;
        if (request.getClientId() != null) {
            client = clientService.get(request.getClientId());
        } else if (request.getNewClient() != null) {
            client = clientService.create(request.getNewClient());
        } else {
            throw new IllegalArgumentException("Either clientId or newClient is required.");
        }

        if (client.getCompany() == null || !currentCompanyId.equals(client.getCompany().getId())) {
            throw new ResourceNotFoundException("Client not found");
        }
        return client;
    }
}
