package org.forecast.backend.service;

import org.forecast.backend.dtos.CreateInvoiceRequest;
import org.forecast.backend.dtos.InvoiceSearchCriteria;
import org.forecast.backend.dtos.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.repository.InvoiceRepository;
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
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Mock
    private ClientService clientService;

    @Mock
    private ExchangeRateService exchangeRateService;

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

    private static Client client(UUID id) {
        Client c = new Client();
        c.setId(id);
        c.setName("Acme");
        c.setEmail("billing@acme.test");
        return c;
    }

    private static Invoice draftInvoice(String invoiceNumber) {
        Invoice inv = new Invoice();
        inv.setId(UUID.randomUUID());
        inv.setInvoiceNumber(invoiceNumber);
        inv.setClient(client(UUID.randomUUID()));
        inv.setSubtotal(new BigDecimal("100.00"));
        inv.setTaxAmount(BigDecimal.ZERO);
        inv.setTotalAmount(new BigDecimal("100.00"));
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
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .taxAmount(new BigDecimal("3.45"))
                .items(List.of(
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Cable")
                                .quantity(2)
                                .unitPrice(new BigDecimal("60.00"))
                                .build(),
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Install")
                                .quantity(1)
                                .unitPrice(new BigDecimal("0.00"))
                                .build()
                ))
                .currency("eur")
                .issueDate(LocalDate.of(2024, 1, 10))
                .dueDate(LocalDate.of(2024, 2, 10))
                .build();

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-2026-00001");
        when(clientService.get(clientId)).thenReturn(client(clientId));
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(new BigDecimal("1.200000"));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice saved = invoiceService.createInvoice(request);

        assertEquals("INV-2026-00001", saved.getInvoiceNumber());
        assertEquals(clientId, saved.getClient().getId());
        assertEquals(new BigDecimal("120.00"), saved.getSubtotal());
        assertEquals(new BigDecimal("3.45"), saved.getTaxAmount());
        assertEquals(new BigDecimal("123.45"), saved.getTotalAmount());
        assertEquals("EUR", saved.getCurrency());
        assertEquals(new BigDecimal("1.200000"), saved.getExchangeRate());

        // base currency is computed from totalAmount
        BigDecimal expectedBase = saved.getTotalAmount().divide(new BigDecimal("1.200000"), 2, RoundingMode.HALF_UP);
        assertEquals(expectedBase, saved.getAmountBaseCurrency());
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
                .taxAmount(BigDecimal.ZERO)
                .items(List.of(
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Service")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-X");
        when(clientService.get(clientId)).thenReturn(client(clientId));
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
                .taxAmount(BigDecimal.ZERO)
                .items(List.of(
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Service")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();

        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-X");
        when(clientService.get(clientId)).thenReturn(client(clientId));
        when(exchangeRateService.getRate("USD", "EUR")).thenReturn(BigDecimal.ZERO);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(request));
        assertEquals("Derived exchange rate must be > 0", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getInvoice_whenPresent_returnsInvoice() {
        Invoice invoice = draftInvoice("INV-1");
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));

        Invoice found = invoiceService.getInvoice("INV-1");

        assertSame(invoice, found);
    }

    @Test
    void getInvoice_whenMissing_throwsResourceNotFound() {
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-404")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice("INV-404"));
        assertTrue(ex.getMessage().contains("No invoice with the number INV-404"));
    }

    @Test
    void getAllInvoices_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice("INV-1")), pageable, 1);
        when(invoiceRepository.findByDeletedFalseOrderByIssueDateDesc(pageable)).thenReturn(page);

        Page<Invoice> result = invoiceService.getAllInvoices(pageable);
        assertSame(page, result);
    }

    @Test
    void getOpenInvoicesByStatus_noArg_usesPageRequest0Size10() {
        Page<Invoice> page = new PageImpl<>(List.of(draftInvoice("INV-1")), PageRequest.of(0, 10), 1);
        when(invoiceRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        List<Invoice> result = invoiceService.getOpenInvoicesByStatus();

        assertEquals(1, result.size());
        verify(invoiceRepository).findByDeletedFalse(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getOpenInvoicesByStatus_withStatus_usesPageRequest0Size10AndDelegates() {
        when(invoiceRepository.findByStatusAndDeletedFalse(eq(InvoiceStatus.SENT), any(Pageable.class)))
                .thenReturn(List.of(draftInvoice("INV-1")));

        List<Invoice> result = invoiceService.getOpenInvoicesByStatus(InvoiceStatus.SENT);

        assertEquals(1, result.size());
        verify(invoiceRepository).findByStatusAndDeletedFalse(eq(InvoiceStatus.SENT), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void sendInvoice_marksAsSent_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));
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
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.payInvoice("INV-1");

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void deleteInvoice_whenDraft_marksDeletedAndSaves() {
        Invoice invoice = draftInvoice("INV-1");
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));

        invoiceService.deleteInvoice("INV-1");

        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertTrue(invoiceCaptor.getValue().isDeleted());
    }

    @Test
    void deleteInvoice_whenNotDraft_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> invoiceService.deleteInvoice("INV-1"));
        assertEquals("Only drafts can be deleted.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_whenAlreadyCancelled_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> invoiceService.cancelInvoice("INV-1"));
        assertEquals("Invoice is already cancelled.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_happyPath_setsCancelledAndSaves() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.cancelInvoice("INV-1");

        assertEquals(InvoiceStatus.CANCELLED, result.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void editInvoice_whenNotDraft_throwsUnsupportedOperationException() {
        Invoice invoice = draftInvoice("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .taxAmount(new BigDecimal("10.00"))
                .build();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> invoiceService.editInvoice("INV-1", request));
        assertEquals("Only drafts can be edited.", ex.getMessage());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void editInvoice_updatesAmountRecomputesBaseAmount_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .taxAmount(new BigDecimal("0.00"))
                .items(List.of(
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Updated")
                                .quantity(1)
                                .unitPrice(new BigDecimal("20.00"))
                                .build()
                ))
                .build();

        Invoice result = invoiceService.editInvoice("INV-1", request);

        assertEquals(new BigDecimal("20.00"), result.getSubtotal());
        assertEquals(new BigDecimal("20.00"), result.getTotalAmount());
        BigDecimal expectedBase = new BigDecimal("20.00").divide(invoice.getExchangeRate(), 2, RoundingMode.HALF_UP);
        assertEquals(expectedBase, result.getAmountBaseCurrency());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void editInvoice_updatesIssueAndDueDates_andSaves() {
        Invoice invoice = draftInvoice("INV-1");
        when(invoiceRepository.findByInvoiceNumberAndDeletedFalse("INV-1")).thenReturn(Optional.of(invoice));
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

        when(invoiceRepository.findByStatusInAndDueDateBeforeAndDeletedFalse(anyList(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page0);

        invoiceService.markOverdueInvoices();

        assertEquals(InvoiceStatus.OVERDUE, inv1.getStatus());
        assertEquals(InvoiceStatus.OVERDUE, inv2.getStatus());

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<Invoice>> listCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(invoiceRepository, times(1)).saveAll(listCaptor.capture());

        List<Invoice> saved = listCaptor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.containsAll(List.of(inv1, inv2)));

        verify(invoiceRepository, times(1)).findByStatusInAndDueDateBeforeAndDeletedFalse(anyList(), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void filterByCriteria_delegatesToRepositoryFindAll_withSpecificationAndPageable() {
        InvoiceSearchCriteria criteria = new InvoiceSearchCriteria();
        Pageable pageable = PageRequest.of(2, 25);
        Page<Invoice> expected = new PageImpl<>(List.of(draftInvoice("INV-1")), pageable, 1);

        when(invoiceRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Invoice>>any(), eq(pageable))).thenReturn(expected);

        Page<Invoice> result = invoiceService.filterByCriteria(criteria, pageable);

        assertSame(expected, result);
        verify(invoiceRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Invoice>>any(), eq(pageable));
    }
}

