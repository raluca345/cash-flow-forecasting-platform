package org.forecast.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.dtos.recurringinvoice.CreateRecurringInvoiceRequest;
import org.forecast.backend.dtos.recurringinvoice.UpdateRecurringInvoiceRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.model.RecurringInvoice;
import org.forecast.backend.model.RecurringInvoiceItem;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.repository.RecurringInvoiceRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.forecast.backend.util.LineItemFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringInvoiceService implements IRecurringInvoiceService {
    private static final int MAX_GENERATION_BATCHES = 10_000;

    private final InvoiceRepository invoiceRepository;
    private final RecurringInvoiceRepository recurringInvoiceRepository;
    private final InvoiceNumberGenerator generator;
    private final ClientService clientService;
    private final ExchangeRateService exchangeRateService;
    private final CompanySecurityService companySecurityService;

    @Value("${app.currency.base:USD}")
    private String baseCurrency;

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private RecurringInvoice getScopedRecurringInvoice(UUID recurringInvoiceId) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        return recurringInvoiceRepository.findByIdAndCompanyId(recurringInvoiceId, currentCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No recurring invoice with id " + recurringInvoiceId + " found."));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    private void replaceTemplateItems(RecurringInvoice recurringInvoice, List<CreateInvoiceItemRequest> items) {
        recurringInvoice.getItems().clear();
        for (CreateInvoiceItemRequest itemRequest : items) {
            if (itemRequest == null) {
                continue;
            }
            recurringInvoice.addItem(LineItemFactory.toRecurringInvoiceItem(itemRequest));
        }
        recurringInvoice.recalculateAmountFromItems();
    }

    private Invoice createInvoiceFromTemplate(
            RecurringInvoice recurringInvoice,
            LocalDate issueDate,
            BigDecimal rateBaseToInvoice
    ) {
        if (recurringInvoice.getClient() == null || recurringInvoice.getCompany() == null) {
            throw new IllegalStateException("Recurring invoice must have a scoped client and company");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generator.generateInvoiceNumber());
        invoice.setCompany(recurringInvoice.getCompany());
        invoice.setClient(recurringInvoice.getClient());
        invoice.setRecurringInvoice(recurringInvoice);
        invoice.setCurrency(recurringInvoice.getCurrency());
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(issueDate.plusDays(recurringInvoice.getPaymentTermsDays()));

        for (RecurringInvoiceItem recurringItem : recurringInvoice.getItems()) {
            if (recurringItem == null) {
                continue;
            }
            invoice.addItem(LineItemFactory.toInvoiceItem(recurringItem));
        }

        invoice.recalculateAmountFromItems();

        if (rateBaseToInvoice == null || rateBaseToInvoice.signum() <= 0) {
            throw new IllegalStateException("Derived exchange rate must be > 0");
        }

        invoice.setExchangeRate(rateBaseToInvoice);
        invoice.setAmountBaseCurrency(
                invoice.getGrossTotal().divide(rateBaseToInvoice, 2, RoundingMode.HALF_UP)
        );

        return invoice;
    }

    private static void ensureScheduleAdvanced(RecurringInvoice recurringInvoice, LocalDate previousIssueDate) {
        if (!recurringInvoice.getNextGenerationDate().isAfter(previousIssueDate)) {
            throw new IllegalStateException(
                    "Recurring invoice " + recurringInvoice.getId() + " did not advance beyond " + previousIssueDate
            );
        }
    }

    @Override
    @Transactional
    public RecurringInvoice create(CreateRecurringInvoiceRequest request) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        Client client = clientService.get(request.getClientId());
        if (client.getCompany() == null || !currentCompanyId.equals(client.getCompany().getId())) {
            throw new ResourceNotFoundException("Client not found");
        }

        validateDateRange(request.getStartDate(), request.getEndDate());

        RecurringInvoice recurringInvoice = new RecurringInvoice();
        recurringInvoice.setCompany(client.getCompany());
        recurringInvoice.setClient(client);
        recurringInvoice.setFrequency(request.getFrequency());
        recurringInvoice.setStartDate(request.getStartDate());
        recurringInvoice.setEndDate(request.getEndDate());
        recurringInvoice.setNextGenerationDate(request.getStartDate());
        recurringInvoice.setCurrency(requireNonBlank(request.getCurrency(), "Currency is required").trim().toUpperCase());
        recurringInvoice.setPaymentTermsDays(request.getPaymentTermsDays() == null ? 30 : request.getPaymentTermsDays());
        recurringInvoice.setDraft(true);
        recurringInvoice.setActive(false);
        replaceTemplateItems(recurringInvoice, request.getItems());

        return recurringInvoiceRepository.save(recurringInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringInvoice get(UUID recurringInvoiceId) {
        return getScopedRecurringInvoice(recurringInvoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurringInvoice> listAll(Pageable pageable) {
        UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Company context required");
        return recurringInvoiceRepository.findByCompanyIdOrderByNextGenerationDateAsc(currentCompanyId, pageable);
    }

    @Override
    @Transactional
    public RecurringInvoice update(UUID recurringInvoiceId, UpdateRecurringInvoiceRequest request) {
        RecurringInvoice recurringInvoice = getScopedRecurringInvoice(recurringInvoiceId);
        if (!recurringInvoice.isDraft()) {
            throw new UnsupportedOperationException("Only draft recurring invoices can be edited.");
        }

        if (request.getClientId() != null) {
            Client client = clientService.get(request.getClientId());
            if (client.getCompany() == null || !recurringInvoice.getCompany().getId().equals(client.getCompany().getId())) {
                throw new ResourceNotFoundException("Client not found");
            }
            recurringInvoice.setClient(client);
        }

        if (request.getFrequency() != null) {
            recurringInvoice.setFrequency(request.getFrequency());
        }
        if (request.getCurrency() != null) {
            recurringInvoice.setCurrency(requireNonBlank(request.getCurrency(), "Currency is required").trim().toUpperCase());
        }
        if (request.getPaymentTermsDays() != null) {
            recurringInvoice.setPaymentTermsDays(request.getPaymentTermsDays());
        }
        if (request.getStartDate() != null) {
            recurringInvoice.setStartDate(request.getStartDate());
            if (recurringInvoice.getNextGenerationDate().isBefore(request.getStartDate())) {
                recurringInvoice.setNextGenerationDate(request.getStartDate());
            }
        }
        if (request.getEndDate() != null) {
            recurringInvoice.setEndDate(request.getEndDate());
        }
        if (request.getNextGenerationDate() != null) {
            recurringInvoice.setNextGenerationDate(request.getNextGenerationDate());
        }
        if (request.getActive() != null) {
            if (request.getActive()) {
                throw new UnsupportedOperationException("Draft recurring invoices must be activated via the activate endpoint.");
            }
            recurringInvoice.setActive(false);
        }
        if (request.getItems() != null) {
            replaceTemplateItems(recurringInvoice, request.getItems());
        }

        validateDateRange(recurringInvoice.getStartDate(), recurringInvoice.getEndDate());
        if (recurringInvoice.getNextGenerationDate().isBefore(recurringInvoice.getStartDate())) {
            throw new IllegalArgumentException("Next generation date must be on or after start date");
        }
        if (recurringInvoice.getNextGenerationDate().isAfter(recurringInvoice.getEndDate())) {
            throw new IllegalArgumentException("Next generation date must be on or before end date");
        }

        return recurringInvoiceRepository.save(recurringInvoice);
    }

    @Override
    @Transactional
    public RecurringInvoice activate(UUID recurringInvoiceId) {
        RecurringInvoice recurringInvoice = getScopedRecurringInvoice(recurringInvoiceId);
        recurringInvoice.setDraft(false);
        recurringInvoice.setActive(true);
        if (recurringInvoice.getNextGenerationDate().isAfter(recurringInvoice.getEndDate())) {
            recurringInvoice.setNextGenerationDate(recurringInvoice.getStartDate());
        }
        return recurringInvoiceRepository.save(recurringInvoice);
    }

    @Override
    @Transactional
    public RecurringInvoice deactivate(UUID recurringInvoiceId) {
        RecurringInvoice recurringInvoice = getScopedRecurringInvoice(recurringInvoiceId);
        recurringInvoice.setActive(false);
        return recurringInvoiceRepository.save(recurringInvoice);
    }

    @Transactional
    @Override
    public void generateDueInvoices() {
        LocalDate today = LocalDate.now();
        int pageSize = 100;
        Map<String, BigDecimal> fxRatesByCurrency = new HashMap<>();

        for (int batch = 0; batch < MAX_GENERATION_BATCHES; batch++) {
            Pageable pageable = PageRequest.of(0, pageSize);
            Page<RecurringInvoice> page = recurringInvoiceRepository.findDueWithLock(today, pageable);
            if (page.isEmpty()) {
                return;
            }
            for (RecurringInvoice recurringInvoice : page.getContent()) {
                LocalDate issueDate = recurringInvoice.getNextGenerationDate();
                if (!invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(recurringInvoice.getId(), issueDate)) {
                    try {
                        BigDecimal rateBaseToInvoice = fxRatesByCurrency.computeIfAbsent(
                                recurringInvoice.getCurrency(),
                                currency -> exchangeRateService.getRate(baseCurrency, currency)
                        );
                        Invoice invoice = createInvoiceFromTemplate(recurringInvoice, issueDate, rateBaseToInvoice);
                        invoiceRepository.saveAndFlush(invoice);
                    } catch (DataIntegrityViolationException exception) {
                        if (!invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(recurringInvoice.getId(), issueDate)) {
                            throw exception;
                        }
                    }
                }
                recurringInvoice.setLastGeneratedAt(issueDate);
                recurringInvoice.advanceNextGenerationDate();
                ensureScheduleAdvanced(recurringInvoice, issueDate);
            }
        }

        throw new IllegalStateException("Recurring invoice generation exceeded " + MAX_GENERATION_BATCHES + " batches");
    }
}
