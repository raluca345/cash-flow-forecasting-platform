package org.forecast.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.enums.LineItemType;
import org.forecast.backend.dtos.recurringinvoice.CreateRecurringInvoiceRequest;
import org.forecast.backend.enums.RecurringInvoiceFrequency;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.model.RecurringInvoice;
import org.forecast.backend.model.RecurringInvoiceItem;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.repository.RecurringInvoiceRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringInvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RecurringInvoiceRepository recurringInvoiceRepository;

    @Mock
    private InvoiceNumberGenerator generator;

    @Mock
    private ClientService clientService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private CompanySecurityService companySecurityService;

    @InjectMocks
    private RecurringInvoiceService recurringInvoiceService;

    private Company company;
    private Client client;
    private CreateRecurringInvoiceRequest createRequest;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());

        client = new Client();
        client.setId(UUID.randomUUID());
        client.setCompany(company);
        client.setName("Acme");

        createRequest = CreateRecurringInvoiceRequest.builder()
                .clientId(client.getId())
                .currency("EUR")
                .frequency(RecurringInvoiceFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .paymentTermsDays(15)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Hosting")
                                .type(LineItemType.SERVICE)
                                .quantity(new BigDecimal("2.000"))
                                .unitPrice(new BigDecimal("10.00"))
                                .vatRatePercent(new BigDecimal("10.000"))
                                .build()
                ))
                .build();
    }

    private RecurringInvoice recurringInvoice() {
        RecurringInvoice recurringInvoice = new RecurringInvoice();
        recurringInvoice.setId(UUID.randomUUID());
        recurringInvoice.setCompany(company);
        recurringInvoice.setClient(client);
        recurringInvoice.setCurrency("EUR");
        recurringInvoice.setFrequency(RecurringInvoiceFrequency.MONTHLY);
        recurringInvoice.setStartDate(LocalDate.now().minusMonths(1));
        recurringInvoice.setEndDate(LocalDate.now());
        recurringInvoice.setNextGenerationDate(LocalDate.now());
        recurringInvoice.setPaymentTermsDays(30);

        RecurringInvoiceItem item = new RecurringInvoiceItem();
        item.setDescription("Subscription");
        item.setType(LineItemType.SUBSCRIPTION);
        item.setQuantity(new BigDecimal("2.000"));
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setVatRatePercent(new BigDecimal("5.000"));
        recurringInvoice.addItem(item);
        recurringInvoice.recalculateAmountFromItems();

        return recurringInvoice;
    }

    @Test
    void create_recurringInvoiceIsItemAuthoritative() {
        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(company.getId());
        when(clientService.get(client.getId())).thenReturn(client);
        when(recurringInvoiceRepository.save(any(RecurringInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringInvoice saved = recurringInvoiceService.create(createRequest);

        assertEquals(company, saved.getCompany());
        assertEquals(client, saved.getClient());
        assertEquals("EUR", saved.getCurrency());
        assertEquals(15, saved.getPaymentTermsDays());
        assertEquals(1, saved.getItems().size());
        assertEquals(0, new BigDecimal("20.00").compareTo(saved.getNetTotal()));
        assertEquals(0, new BigDecimal("2.00").compareTo(saved.getVatTotal()));
        assertEquals(0, new BigDecimal("22.00").compareTo(saved.getGrossTotal()));
        assertEquals(LineItemType.SERVICE, saved.getItems().get(0).getType());
        assertTrue(saved.isDraft());
        assertFalse(saved.isActive());
        assertEquals(saved, saved.getItems().get(0).getRecurringInvoice());
    }

    @Test
    void generateDueInvoices_createsCompanyScopedInvoiceWithTemplateItemsAndCurrentFx() {
        RecurringInvoice recurringInvoice = recurringInvoice();
        ReflectionTestUtils.setField(recurringInvoiceService, "baseCurrency", "USD");

        when(generator.generateInvoiceNumber()).thenReturn("INV-2026-00001");
        when(recurringInvoiceRepository.findDueWithLock(any(LocalDate.class), any()))
                .thenReturn(new PageImpl<>(List.of(recurringInvoice), PageRequest.of(0, 100), 1))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        when(invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(recurringInvoice.getId(), recurringInvoice.getNextGenerationDate()))
                .thenReturn(false);
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(new BigDecimal("2.000000"));
        when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recurringInvoiceService.generateDueInvoices();

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, times(1)).saveAndFlush(invoiceCaptor.capture());

        Invoice saved = invoiceCaptor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals(client, saved.getClient());
        assertEquals(recurringInvoice, saved.getRecurringInvoice());
        assertEquals("EUR", saved.getCurrency());
        assertEquals(0, new BigDecimal("40.00").compareTo(saved.getNetTotal()));
        assertEquals(0, new BigDecimal("2.00").compareTo(saved.getVatTotal()));
        assertEquals(0, new BigDecimal("42.00").compareTo(saved.getGrossTotal()));
        assertEquals(0, new BigDecimal("21.00").compareTo(saved.getAmountBaseCurrency()));
        assertEquals(1, saved.getItems().size());
        assertEquals(LineItemType.SUBSCRIPTION, saved.getItems().get(0).getType());
        assertNotNull(saved.getItems().get(0).getInvoice());
        assertEquals(recurringInvoice.getEndDate(), recurringInvoice.getLastGeneratedAt());
        assertFalse(recurringInvoice.isActive());
        verify(exchangeRateService, times(1)).getRate(eq("USD"), eq("EUR"));
    }

    @Test
    void generateDueInvoices_skipsDuplicateIssueDateButStillAdvancesSchedule() {
        RecurringInvoice recurringInvoice = recurringInvoice();
        LocalDate issueDate = recurringInvoice.getNextGenerationDate();

        when(recurringInvoiceRepository.findDueWithLock(any(LocalDate.class), any()))
                .thenReturn(new PageImpl<>(List.of(recurringInvoice), PageRequest.of(0, 100), 1))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        when(invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(recurringInvoice.getId(), issueDate))
                .thenReturn(true);

        recurringInvoiceService.generateDueInvoices();

        verify(invoiceRepository, times(0)).saveAndFlush(any(Invoice.class));
        verify(exchangeRateService, times(0)).getRate(any(), any());
        assertEquals(issueDate.plusMonths(1), recurringInvoice.getNextGenerationDate());
        assertEquals(issueDate, recurringInvoice.getLastGeneratedAt());
        assertFalse(recurringInvoice.isActive());
    }

    @Test
    void generateDueInvoices_reusesFxRateForTemplatesWithSameCurrency() {
        RecurringInvoice first = recurringInvoice();
        RecurringInvoice second = recurringInvoice();
        second.setId(UUID.randomUUID());
        ReflectionTestUtils.setField(recurringInvoiceService, "baseCurrency", "USD");

        when(generator.generateInvoiceNumber()).thenReturn("INV-2026-00001", "INV-2026-00002");
        when(recurringInvoiceRepository.findDueWithLock(any(LocalDate.class), any()))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 100), 2))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        when(invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(first.getId(), first.getNextGenerationDate()))
                .thenReturn(false);
        when(invoiceRepository.existsByRecurringInvoiceIdAndIssueDate(second.getId(), second.getNextGenerationDate()))
                .thenReturn(false);
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(new BigDecimal("2.000000"));
        when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recurringInvoiceService.generateDueInvoices();

        verify(exchangeRateService, times(1)).getRate("USD", "EUR");
        verify(invoiceRepository, times(2)).saveAndFlush(any(Invoice.class));
    }

    @Test
    void update_nonDraftRecurringInvoiceThrows() {
        RecurringInvoice recurringInvoice = recurringInvoice();
        recurringInvoice.setDraft(false);

        when(companySecurityService.requireCurrentCompanyId(any())).thenReturn(company.getId());
        when(recurringInvoiceRepository.findByIdAndCompanyId(recurringInvoice.getId(), company.getId()))
                .thenReturn(java.util.Optional.of(recurringInvoice));

        assertThrows(
                UnsupportedOperationException.class,
                () -> recurringInvoiceService.update(recurringInvoice.getId(), org.forecast.backend.dtos.recurringinvoice.UpdateRecurringInvoiceRequest.builder().build())
        );
    }
}
