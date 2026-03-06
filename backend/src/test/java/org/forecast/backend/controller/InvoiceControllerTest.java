package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forecast.backend.config.GlobalExceptionHandler;
import org.forecast.backend.config.TestConfig;
import org.forecast.backend.dtos.CreateInvoiceRequest;
import org.forecast.backend.dtos.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.service.IInvoiceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InvoiceController.class)
@Import({GlobalExceptionHandler.class, TestConfig.class})
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IInvoiceService invoiceService;

    private static Invoice sampleInvoice(String invoiceNumber) {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setName("Acme Ltd");
        client.setEmail("billing@acme.test");

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setClient(client);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setCurrency("USD");
        invoice.setAmountBaseCurrency(new BigDecimal("100.00"));
        invoice.setExchangeRate(new BigDecimal("1.000000"));
        invoice.setIssueDate(LocalDate.of(2024, 1, 10));
        invoice.setDueDate(LocalDate.of(2024, 2, 10));
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSentAt(null);
        invoice.setPaidAt(null);
        return invoice;
    }

    private static CreateInvoiceRequest validCreateInvoiceRequest() {
        return CreateInvoiceRequest.builder()
                .clientId(UUID.randomUUID())
                .taxRatePercent(new BigDecimal("7.500"))
                .items(List.of(
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Cable subscription")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("50.00"))
                                .build(),
                        org.forecast.backend.dtos.CreateInvoiceItemRequest.builder()
                                .description("Installation")
                                .quantity(new BigDecimal("1.000"))
                                .unitPrice(new BigDecimal("30.00"))
                                .build()
                ))
                .currency("USD")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .build();
    }

    @Test
    void createInvoice_returnsInvoiceResponse() throws Exception {
        CreateInvoiceRequest request = validCreateInvoiceRequest();
        Invoice invoice = sampleInvoice("INV-001");

        when(invoiceService.createInvoice(any(CreateInvoiceRequest.class))).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.clientName").value("Acme Ltd"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.issueDate").value("2024-01-10"))
                .andExpect(jsonPath("$.dueDate").value("2024-02-10"));

        verify(invoiceService, times(1)).createInvoice(any(CreateInvoiceRequest.class));
    }

    @Test
    void createInvoice_invalidBody_returnsValidationError() throws Exception {
        CreateInvoiceRequest invalidRequest = CreateInvoiceRequest.builder()
                .clientId(null)
                .taxRatePercent(new BigDecimal("-0.01"))
                .items(List.of())
                .currency("usd")
                .issueDate(LocalDate.now().plusDays(1))
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.clientId").value("Client ID is required"))
                .andExpect(jsonPath("$.fieldErrors.taxRatePercent").value("Tax rate must be >= 0"))
                .andExpect(jsonPath("$.fieldErrors.items").value("At least one item is required"))
                .andExpect(jsonPath("$.fieldErrors.currency", anyOf(
                        is("Currency must be uppercase 3 letters (e.g. USD)"),
                        is("Currency must be a 3-letter ISO code"),
                        is("Currency is required"))))
                .andExpect(jsonPath("$.fieldErrors.issueDate").value("Issue date cannot be in the future"))
                .andExpect(jsonPath("$.fieldErrors.dueDate").value("Due date must be today or in the future"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void createInvoice_serviceThrowsIllegalArgument_returnsInvalidArgumentError() throws Exception {
        CreateInvoiceRequest request = validCreateInvoiceRequest();
        when(invoiceService.createInvoice(any(CreateInvoiceRequest.class)))
                .thenThrow(new IllegalArgumentException("Bad currency"));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Bad currency"));
    }

    @Test
    void getInvoice_returnsInvoiceResponse() throws Exception {
        when(invoiceService.getInvoice("INV-123")).thenReturn(sampleInvoice("INV-123"));

        mockMvc.perform(get("/api/v1/invoices/INV-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-123"));

        verify(invoiceService).getInvoice("INV-123");
    }

    @Test
    void getInvoice_notFound_returns404ErrorResponse() throws Exception {
        when(invoiceService.getInvoice("INV-404")).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(get("/api/v1/invoices/INV-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice not found"));

        verify(invoiceService).getInvoice("INV-404");
    }

    @Test
    void getAllInvoices_usesDefaultPageable() throws Exception {
        Page<Invoice> page = new PageImpl<>(List.of(sampleInvoice("INV-001")),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "issueDate")),
                1);

        when(invoiceService.getAllInvoices(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceService).getAllInvoices(captor.capture());
        Pageable used = captor.getValue();
        // defaults from @PageableDefault
        Assertions.assertEquals(0, used.getPageNumber());
        Assertions.assertEquals(10, used.getPageSize());
        Sort.Order issueDateOrder = used.getSort().getOrderFor("issueDate");
        Assertions.assertNotNull(issueDateOrder);
        Assertions.assertEquals(Sort.Direction.DESC, issueDateOrder.getDirection());
    }

    @Test
    void getAllInvoices_customPageable_isPassedThrough() throws Exception {
        Page<Invoice> page = new PageImpl<>(List.of(sampleInvoice("INV-002")),
                PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "dueDate")),
                6);
        when(invoiceService.getAllInvoices(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("sort", "dueDate,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceService).getAllInvoices(captor.capture());
        Pageable used = captor.getValue();
        Assertions.assertEquals(1, used.getPageNumber());
        Assertions.assertEquals(5, used.getPageSize());
        Sort.Order dueDateOrder = used.getSort().getOrderFor("dueDate");
        Assertions.assertNotNull(dueDateOrder);
        Assertions.assertEquals(Sort.Direction.ASC, dueDateOrder.getDirection());
    }

    @Test
    void patchSendInvoice_returnsInvoiceResponse() throws Exception {
        Invoice sent = sampleInvoice("INV-001");
        sent.setStatus(InvoiceStatus.SENT);
        sent.setSentAt(Instant.parse("2024-01-11T10:15:30Z"));

        when(invoiceService.sendInvoice("INV-001")).thenReturn(sent);

        mockMvc.perform(patch("/api/v1/invoices/INV-001/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").value("2024-01-11T10:15:30Z"));

        verify(invoiceService).sendInvoice("INV-001");
    }

    @Test
    void patchSendInvoice_notFound_returns404() throws Exception {
        when(invoiceService.sendInvoice("INV-404")).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(patch("/api/v1/invoices/INV-404/send"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(invoiceService).sendInvoice("INV-404");
    }

    @Test
    void patchSendInvoice_illegalArgument_returns400() throws Exception {
        when(invoiceService.sendInvoice("INV-001")).thenThrow(new IllegalArgumentException("Cannot send"));

        mockMvc.perform(patch("/api/v1/invoices/INV-001/send"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Cannot send"));

        verify(invoiceService).sendInvoice("INV-001");
    }

    @Test
    void patchPayInvoice_returnsInvoiceResponse() throws Exception {
        Invoice paid = sampleInvoice("INV-010");
        paid.setStatus(InvoiceStatus.PAID);
        paid.setPaidAt(Instant.parse("2024-01-12T10:15:30Z"));

        when(invoiceService.payInvoice("INV-010")).thenReturn(paid);

        mockMvc.perform(patch("/api/v1/invoices/INV-010/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").value("2024-01-12T10:15:30Z"));

        verify(invoiceService).payInvoice("INV-010");
    }

    @Test
    void patchPayInvoice_notFound_returns404() throws Exception {
        when(invoiceService.payInvoice("INV-404")).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(patch("/api/v1/invoices/INV-404/pay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(invoiceService).payInvoice("INV-404");
    }

    @Test
    void patchPayInvoice_illegalArgument_returns400() throws Exception {
        when(invoiceService.payInvoice("INV-010")).thenThrow(new IllegalArgumentException("Cannot pay"));

        mockMvc.perform(patch("/api/v1/invoices/INV-010/pay"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Cannot pay"));

        verify(invoiceService).payInvoice("INV-010");
    }

    @Test
    void patchCancelInvoice_returnsInvoiceResponse() throws Exception {
        Invoice cancelled = sampleInvoice("INV-020");
        cancelled.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceService.cancelInvoice("INV-020")).thenReturn(cancelled);

        mockMvc.perform(patch("/api/v1/invoices/INV-020/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(invoiceService).cancelInvoice("INV-020");
    }

    @Test
    void patchCancelInvoice_notFound_returns404() throws Exception {
        when(invoiceService.cancelInvoice("INV-404")).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(patch("/api/v1/invoices/INV-404/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(invoiceService).cancelInvoice("INV-404");
    }

    @Test
    void patchCancelInvoice_illegalArgument_returns400() throws Exception {
        when(invoiceService.cancelInvoice("INV-020")).thenThrow(new IllegalArgumentException("Cannot cancel"));

        mockMvc.perform(patch("/api/v1/invoices/INV-020/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Cannot cancel"));

        verify(invoiceService).cancelInvoice("INV-020");
    }

    @Test
    void editInvoice_allowsEmptyBody_andReturnsInvoiceResponse() throws Exception {
        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder().build();
        Invoice updated = sampleInvoice("INV-030");
        updated.setSubtotal(new BigDecimal("200.00"));

        when(invoiceService.editInvoice(eq("INV-030"), any(UpdateInvoiceDraftPartialRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/invoices/INV-030")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-030"));

        verify(invoiceService).editInvoice(eq("INV-030"), any(UpdateInvoiceDraftPartialRequest.class));
    }

    @Test
    void editInvoice_bodyValidationError_returns400() throws Exception {
        UpdateInvoiceDraftPartialRequest invalid = UpdateInvoiceDraftPartialRequest.builder()
                .taxRatePercent(new BigDecimal("-0.01"))
                .issueDate(LocalDate.now().plusDays(1))
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        mockMvc.perform(patch("/api/v1/invoices/INV-030")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.taxRatePercent").value("Tax rate must be >= 0"))
                .andExpect(jsonPath("$.fieldErrors.issueDate").value("Issue date cannot be in the future"))
                .andExpect(jsonPath("$.fieldErrors.dueDate").value("Due date must be today or in the future"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void editInvoice_notFound_returns404() throws Exception {
        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .taxRatePercent(new BigDecimal("10.000"))
                .build();
        when(invoiceService.editInvoice(eq("INV-404"), any(UpdateInvoiceDraftPartialRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(patch("/api/v1/invoices/INV-404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(invoiceService).editInvoice(eq("INV-404"), any(UpdateInvoiceDraftPartialRequest.class));
    }

    @Test
    void editInvoice_illegalArgument_returns400() throws Exception {
        UpdateInvoiceDraftPartialRequest request = UpdateInvoiceDraftPartialRequest.builder()
                .taxRatePercent(new BigDecimal("10.000"))
                .build();
        when(invoiceService.editInvoice(eq("INV-030"), any(UpdateInvoiceDraftPartialRequest.class)))
                .thenThrow(new IllegalArgumentException("Not a draft"));

        mockMvc.perform(patch("/api/v1/invoices/INV-030")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Not a draft"));

        verify(invoiceService).editInvoice(eq("INV-030"), any(UpdateInvoiceDraftPartialRequest.class));
    }

    @Test
    void deleteInvoice_returns204() throws Exception {
        doNothing().when(invoiceService).deleteInvoice("INV-001");

        mockMvc.perform(delete("/api/v1/invoices/INV-001"))
                .andExpect(status().isNoContent());

        verify(invoiceService).deleteInvoice("INV-001");
    }

    @Test
    void deleteInvoice_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Invoice not found"))
                .when(invoiceService).deleteInvoice("INV-404");

        mockMvc.perform(delete("/api/v1/invoices/INV-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(invoiceService).deleteInvoice("INV-404");
    }

    @Test
    void deleteInvoice_illegalArgument_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Cannot delete"))
                .when(invoiceService).deleteInvoice("INV-001");

        mockMvc.perform(delete("/api/v1/invoices/INV-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Cannot delete"));

        verify(invoiceService).deleteInvoice("INV-001");
    }

    @Test
    void searchInvoices_bindsCriteriaAndReturnsPaginatedResponse() throws Exception {
        Page<Invoice> page = new PageImpl<>(List.of(sampleInvoice("INV-777")),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "issueDate")),
                1);
        when(invoiceService.filterByCriteria(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("status", "DRAFT")
                        .queryParam("clientName", "Acme")
                        .queryParam("currency", "USD")
                        .queryParam("minAmount", "10.00")
                        .queryParam("dueDateFrom", "2024-01-01")
                        .queryParam("dueDateTo", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-777"));

        verify(invoiceService).filterByCriteria(any(), any(Pageable.class));
    }

    @Test
    void searchInvoices_usesDefaultPageable() throws Exception {
        Page<Invoice> page = new PageImpl<>(List.of(sampleInvoice("INV-777")),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "issueDate")),
                1);
        when(invoiceService.filterByCriteria(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices/search"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceService).filterByCriteria(any(), captor.capture());
        Pageable used = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, used.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, used.getPageSize());
        Sort.Order searchIssueDateOrder = used.getSort().getOrderFor("issueDate");
        org.junit.jupiter.api.Assertions.assertNotNull(searchIssueDateOrder);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, searchIssueDateOrder.getDirection());
    }

    @Test
    void searchInvoices_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("clientId", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }

    @Test
    void searchInvoices_invalidDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("dueDateFrom", "2024-13-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }

    @Test
    void searchInvoices_invalidBigDecimal_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("minAmount", "ten"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }

    @Test
    void searchInvoices_invalidBoolean_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("overdue", "notabool"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }

    @Test
    void searchInvoices_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(invoiceService.filterByCriteria(any(), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Bad criteria"));

        mockMvc.perform(get("/api/v1/invoices/search")
                        .queryParam("currency", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Bad criteria"));

        verify(invoiceService).filterByCriteria(any(), any(Pageable.class));
    }
}
