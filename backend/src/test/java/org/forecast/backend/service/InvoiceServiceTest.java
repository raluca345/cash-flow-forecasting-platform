package org.forecast.backend.service;

import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.dtos.invoice.CreateInvoiceRequest;
import org.forecast.backend.dtos.invoice.InvoiceSearchCriteria;
import org.forecast.backend.dtos.invoice.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.repository.InvoiceRepository;
import org.forecast.backend.repository.CompanyRepository;
import org.forecast.backend.util.InvoiceNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CompanySecurityService companySecurityService;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Mock
    private ClientService clientService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceService, "baseCurrency", "USD");
    }

    private UUID testCompanyId;

    @BeforeEach
    void setCompanyContext() {
        testCompanyId = UUID.randomUUID();
        when(companySecurityService.getCurrentCompanyId()).thenReturn(testCompanyId);
    }

    private static Client client(UUID id, UUID companyId) {
        Client c = new Client();
        c.setId(id);
        c.setName("Acme");
        c.setEmail("billing@acme.test");
        Company company = new Company();
        company.setId(companyId);
        c.setCompany(company);
        return c;
    }

    private static Invoice draftInvoice(String invoiceNumber) {
        Invoice inv = new Invoice();
        inv.setId(UUID.randomUUID());
        inv.setInvoiceNumber(invoiceNumber);
        inv.setClient(client(UUID.randomUUID(), UUID.randomUUID()));
        inv.setNetTotal(new BigDecimal("100.00"));
        inv.setVatTotal(BigDecimal.ZERO);
        inv.setGrossTotal(new BigDecimal("100.00"));
        inv.setCurrency("USD");
        inv.setExchangeRate(new BigDecimal("2.000000")); // 1 USD(base) = 2 EUR(invoice) style
        inv.setAmountBaseCurrency(new BigDecimal("50.00"));
        inv.setIssueDate(LocalDate.of(2024, 1, 1));
        inv.setDueDate(LocalDate.of(2024, 2, 1));
        inv.setStatus(InvoiceStatus.DRAFT);
        inv.setDeleted(false);
        return inv;
    }

    @Test
    void createInvoice_happyPath_persistsInvoiceWithDerivedBaseAmount() {
        UUID clientId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Cable")
                                .quantity(new BigDecimal("2.000"))
                                .unitPrice(new BigDecimal("60.00"))
                                .vatRatePercent(new BigDecimal("2.875"))
                                .build(),
                        CreateInvoiceItemRequest.builder()
                                .description("Install")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("0.00"))
                                .vatRatePercent(new BigDecimal("2.875"))
                                .build()
                ))
                .currency("eur")
                .issueDate(LocalDate.of(2024, 1, 10))
                .dueDate(LocalDate.of(2024, 2, 10))
                .build();
        request.setCompanyId(companyId);

        Company company = new Company();
        company.setId(companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companySecurityService.getCurrentCompanyId()).thenReturn(companyId);

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-2026-00001");
        when(clientService.get(clientId)).thenReturn(client(clientId, companyId));
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(new BigDecimal("1.200000"));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice saved = invoiceService.createInvoice(request);

        assertEquals("INV-2026-00001", saved.getInvoiceNumber());
        assertEquals(clientId, saved.getClient().getId());

        // Net line totals: 2*60.00 + 1*0.00 = 120.00
        // VAT: 120.00 * 2.875% = 3.45
        // Net total (sum of net amounts): 120.00
        // Gross total (netTotal + vatTotal): 123.45
        assertEquals(0, saved.getNetTotal().compareTo(new BigDecimal("120.00")));
        assertEquals(0, saved.getVatTotal().compareTo(new BigDecimal("3.45")));
        assertEquals(0, saved.getGrossTotal().compareTo(new BigDecimal("123.45")));

        // base currency is computed from grossTotal
        BigDecimal expectedBase = saved.getGrossTotal().divide(new BigDecimal("1.200000"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expectedBase.compareTo(saved.getAmountBaseCurrency()));
        assertEquals(request.getIssueDate(), saved.getIssueDate());
        assertEquals(request.getDueDate(), saved.getDueDate());
        assertFalse(saved.isDeleted());

        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoice_exchangeRateNull_throwsIllegalStateException() {
        UUID clientId = UUID.randomUUID();
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Service")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("10.00"))
                                .vatRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();
        UUID companyId = UUID.randomUUID();
        request.setCompanyId(companyId);
        Company company = new Company();
        company.setId(companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companySecurityService.getCurrentCompanyId()).thenReturn(companyId);

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-X");
        when(clientService.get(clientId)).thenReturn(client(clientId, companyId));
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(request));
        assertEquals("Derived exchange rate must be > 0", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_exchangeRateNonPositive_throwsIllegalStateException() {
        UUID clientId = UUID.randomUUID();
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Service")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("10.00"))
                                .vatRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();
        UUID companyId = UUID.randomUUID();
        request.setCompanyId(companyId);
        Company company = new Company();
        company.setId(companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companySecurityService.getCurrentCompanyId()).thenReturn(companyId);

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-X");
        when(clientService.get(clientId)).thenReturn(client(clientId, companyId));
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(BigDecimal.ZERO);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(request));
        assertEquals("Derived exchange rate must be > 0", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getInvoice_whenPresent_returnsInvoice() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));

        Invoice found = invoiceService.getInvoice("INV-1");

        assertSame(invoice, found);
    }

    @Test
    void getInvoice_whenMissing_throwsResourceNotFound() {
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-404", testCompanyId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice("INV-404"));
        assertTrue(ex.getMessage().contains("No invoice with the number INV-404"));
    }

    @Test
    void getAllInvoices_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice("INV-1")), pageable, 1);
        when(invoiceRepository.findByCompanyIdAndDeletedFalseOrderByIssueDateDesc(testCompanyId, pageable)).thenReturn(page);

        Page<Invoice> result = invoiceService.getAllInvoices(pageable);
        assertSame(page, result);
    }


    @Test
    void sendInvoice_marksAsSent_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.sendInvoice("INV-1");

        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertNotNull(result.getSentAt());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void payInvoice_marksAsPaid_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.payInvoice("INV-1");

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void deleteInvoice_whenDraft_marksDeletedAndSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));

        invoiceService.deleteInvoice("INV-1");

        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertTrue(invoiceCaptor.getValue().isDeleted());
    }

    @Test
    void deleteInvoice_whenNotDraft_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> invoiceService.deleteInvoice("INV-1"));
        assertEquals("Only drafts can be deleted.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_whenAlreadyCancelled_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> invoiceService.cancelInvoice("INV-1"));
        assertEquals("Invoice is already cancelled.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_happyPath_setsCancelledAndSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.cancelInvoice("INV-1");

        assertEquals(InvoiceStatus.CANCELLED, result.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void editInvoice_whenNotDraft_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .build();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> invoiceService.editInvoice("INV-1", request));
        assertEquals("Only drafts can be edited.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void editInvoice_updatesAmountRecomputesBaseAmount_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Updated")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("20.00"))
                                .vatRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        Invoice result = invoiceService.editInvoice("INV-1", request);

        assertEquals(0, result.getNetTotal().compareTo(new BigDecimal("20.00")));
        assertEquals(0, result.getVatTotal().compareTo(new BigDecimal("0.00")));
        assertEquals(0, result.getGrossTotal().compareTo(new BigDecimal("20.00")));
        BigDecimal expectedBase = new BigDecimal("20.00").divide(invoice.getExchangeRate(), 2, RoundingMode.HALF_UP);
        assertEquals(0, expectedBase.compareTo(result.getAmountBaseCurrency()));
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void editInvoice_updatesIssueAndDueDates_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .issueDate(LocalDate.of(2024, 3, 1))
                .dueDate(LocalDate.of(2024, 4, 1))
                .build();

        Invoice result = invoiceService.editInvoice("INV-1", request);

        assertEquals(LocalDate.of(2024, 3, 1), result.getIssueDate());
        assertEquals(LocalDate.of(2024, 4, 1), result.getDueDate());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markOverdueInvoices_marksInvoicesAndSavesAll_untilNoNextPage() {
        Invoice inv1 = draftInvoice("INV-1");
        inv1.setStatus(InvoiceStatus.SENT);
        inv1.setDueDate(LocalDate.now().minusDays(1));

        Invoice inv2 = draftInvoice("INV-2");
        inv2.setStatus(InvoiceStatus.SENT);
        inv2.setDueDate(LocalDate.now().minusDays(2));

        Page<Invoice> page0 = new PageImpl<>(List.of(inv1, inv2), PageRequest.of(0, 100), 2);

        when(invoiceRepository.findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(eq(testCompanyId), anyList(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page0)
                .thenReturn(Page.empty());

        invoiceService.markOverdueInvoices();

        assertEquals(InvoiceStatus.OVERDUE, inv1.getStatus());
        assertEquals(InvoiceStatus.OVERDUE, inv2.getStatus());

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<Invoice>> listCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(invoiceRepository, times(1)).saveAll(listCaptor.capture());

        List<Invoice> saved = listCaptor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.containsAll(List.of(inv1, inv2)));

        verify(invoiceRepository, times(2)).findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(eq(testCompanyId), anyList(), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filterByCriteria_delegatesToRepositoryFindAll_withSpecificationAndPageable() {
        InvoiceSearchCriteria criteria = new InvoiceSearchCriteria();
        Pageable pageable = PageRequest.of(2, 25);
        Page<Invoice> expected = new PageImpl<>(List.of(draftInvoice("INV-1")), pageable, 1);

        when(invoiceRepository.findAll((Specification<Invoice>) any(), eq(pageable))).thenReturn(expected);

        Page<Invoice> result = invoiceService.filterByCriteria(criteria, pageable);

        assertSame(expected, result);
        verify(invoiceRepository).findAll((Specification<Invoice>) any(), eq(pageable));
    }

    @Test
    void createInvoice_whenRequestTargetsDifferentCompany_throwsAccessDenied() {
        UUID clientId = UUID.randomUUID();
        UUID requestedCompanyId = UUID.randomUUID();
        UUID authenticatedCompanyId = UUID.randomUUID();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Service")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("10.00"))
                                .vatRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .companyId(requestedCompanyId)
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();

        when(companySecurityService.getCurrentCompanyId()).thenReturn(authenticatedCompanyId);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> invoiceService.createInvoice(request));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void filterByCriteria_whenRequestTargetsDifferentCompany_throwsAccessDenied() {
        InvoiceSearchCriteria criteria = new InvoiceSearchCriteria();
        criteria.setCompanyId(UUID.randomUUID());

        when(companySecurityService.getCurrentCompanyId()).thenReturn(UUID.randomUUID());

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> invoiceService.filterByCriteria(criteria, PageRequest.of(0, 10))
        );

        verify(invoiceRepository, never()).findAll((Specification<Invoice>) any(), any(Pageable.class));
    }

    @Test
    void markOverdueInvoices_withoutAuthenticatedCompany_processesAllCompanies() {
        UUID companyAId = UUID.randomUUID();
        UUID companyBId = UUID.randomUUID();

        Company companyA = new Company();
        companyA.setId(companyAId);

        Company companyB = new Company();
        companyB.setId(companyBId);

        Invoice inv1 = draftInvoice("INV-A");
        inv1.setStatus(InvoiceStatus.SENT);
        inv1.setDueDate(LocalDate.now().minusDays(1));

        Invoice inv2 = draftInvoice("INV-B");
        inv2.setStatus(InvoiceStatus.SENT);
        inv2.setDueDate(LocalDate.now().minusDays(2));

        when(companySecurityService.getCurrentCompanyId()).thenReturn(null);
        when(companyRepository.findAll()).thenReturn(List.of(companyA, companyB));
        when(invoiceRepository.findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(eq(companyAId), anyList(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inv1), PageRequest.of(0, 100), 1))
                .thenReturn(Page.empty());
        when(invoiceRepository.findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(eq(companyBId), anyList(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inv2), PageRequest.of(0, 100), 1))
                .thenReturn(Page.empty());

        invoiceService.markOverdueInvoices();

        assertEquals(InvoiceStatus.OVERDUE, inv1.getStatus());
        assertEquals(InvoiceStatus.OVERDUE, inv2.getStatus());
        verify(invoiceRepository, times(2)).saveAll(anyList());
    }

    @Test
    void editInvoice_whenItemsCleared_recalculatesSubtotalToZero_andTotalToTax() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setNetTotal(new BigDecimal("100.00"));
        invoice.setVatTotal(new BigDecimal("5.00"));
        invoice.setGrossTotal(new BigDecimal("105.00"));

        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .items(List.of())
                .build();

        Invoice result = invoiceService.editInvoice("INV-1", request);

        assertEquals(0, result.getNetTotal().compareTo(new BigDecimal("0.00")));
        assertEquals(0, result.getVatTotal().compareTo(new BigDecimal("0.00")));
        assertEquals(0, result.getGrossTotal().compareTo(new BigDecimal("0.00")));

        BigDecimal expectedBase = result.getGrossTotal().divide(invoice.getExchangeRate(), 2, RoundingMode.HALF_UP);
        assertEquals(0, expectedBase.compareTo(result.getAmountBaseCurrency()));

        verify(invoiceRepository).save(invoice);
    }

    @Test
    void editInvoice_whenVatRateZero_recalculatesVatAsZero() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setCompany(new Company());
        invoice.getCompany().setId(testCompanyId);
        when(invoiceRepository.findByInvoiceNumberAndCompanyIdAndDeletedFalse("INV-1", testCompanyId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Updated")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("20.00"))
                                .vatRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        Invoice result = invoiceService.editInvoice("INV-1", request);

        assertEquals(0, result.getNetTotal().compareTo(new BigDecimal("20.00")));
        assertEquals(0, result.getVatTotal().compareTo(new BigDecimal("0.00")));
        assertEquals(0, result.getGrossTotal().compareTo(new BigDecimal("20.00")));

        verify(invoiceRepository).save(invoice);
    }
}
