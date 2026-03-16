package org.forecast.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.dtos.recurringinvoice.CreateRecurringInvoiceRequest;
import org.forecast.backend.dtos.recurringinvoice.UpdateRecurringInvoiceRequest;
import org.forecast.backend.enums.RecurringInvoiceFrequency;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Client;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.RecurringInvoice;
import org.forecast.backend.model.RecurringInvoiceItem;
import org.forecast.backend.service.IRecurringInvoiceService;
import org.forecast.backend.service.JwtService;
import org.forecast.backend.testing.WebMvcTestWithTestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecurringInvoiceController.class)
@WebMvcTestWithTestSecurity
class RecurringInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IRecurringInvoiceService recurringInvoiceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static RecurringInvoice recurringInvoice(UUID recurringInvoiceId) {
        Company company = new Company();
        company.setId(UUID.randomUUID());

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setName("Acme");
        client.setCompany(company);

        RecurringInvoice recurringInvoice = new RecurringInvoice();
        recurringInvoice.setId(recurringInvoiceId);
        recurringInvoice.setCompany(company);
        recurringInvoice.setClient(client);
        recurringInvoice.setCurrency("EUR");
        recurringInvoice.setFrequency(RecurringInvoiceFrequency.MONTHLY);
        recurringInvoice.setStartDate(LocalDate.of(2024, 1, 1));
        recurringInvoice.setEndDate(LocalDate.of(2024, 12, 31));
        recurringInvoice.setNextGenerationDate(LocalDate.of(2024, 2, 1));
        recurringInvoice.setPaymentTermsDays(30);
        recurringInvoice.setActive(true);

        RecurringInvoiceItem item = new RecurringInvoiceItem();
        item.setDescription("Hosting");
        item.setQuantity(new BigDecimal("2.000"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setVatRatePercent(new BigDecimal("5.000"));
        recurringInvoice.addItem(item);
        recurringInvoice.recalculateAmountFromItems();

        return recurringInvoice;
    }

    @Test
    void createRecurringInvoice_returnsResponse() throws Exception {
        UUID recurringInvoiceId = UUID.randomUUID();
        CreateRecurringInvoiceRequest request = CreateRecurringInvoiceRequest.builder()
                .clientId(UUID.randomUUID())
                .currency("EUR")
                .frequency(RecurringInvoiceFrequency.MONTHLY)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .paymentTermsDays(30)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .description("Hosting")
                                .quantity(new BigDecimal("2.000"))
                                .unitPrice(new BigDecimal("10.00"))
                                .vatRatePercent(new BigDecimal("5.000"))
                                .build()
                ))
                .build();

        when(recurringInvoiceService.create(any(CreateRecurringInvoiceRequest.class))).thenReturn(recurringInvoice(recurringInvoiceId));

        mockMvc.perform(post("/api/v1/recurring-invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recurringInvoiceId.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.grossTotal").value(21.00));

        verify(recurringInvoiceService).create(any(CreateRecurringInvoiceRequest.class));
    }

    @Test
    void createRecurringInvoice_validationError_returns400() throws Exception {
        CreateRecurringInvoiceRequest request = CreateRecurringInvoiceRequest.builder()
                .clientId(null)
                .items(List.of())
                .currency("eur")
                .build();

        mockMvc.perform(post("/api/v1/recurring-invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(recurringInvoiceService);
    }

    @Test
    void listRecurringInvoices_returnsPaginatedResponse() throws Exception {
        UUID recurringInvoiceId = UUID.randomUUID();
        when(recurringInvoiceService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recurringInvoice(recurringInvoiceId)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/recurring-invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(recurringInvoiceId.toString()));
    }

    @Test
    void getRecurringInvoice_notFound_returns404() throws Exception {
        UUID recurringInvoiceId = UUID.randomUUID();
        when(recurringInvoiceService.get(recurringInvoiceId))
                .thenThrow(new ResourceNotFoundException("No recurring invoice"));

        mockMvc.perform(get("/api/v1/recurring-invoices/{id}", recurringInvoiceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateRecurringInvoice_returnsUpdatedResponse() throws Exception {
        UUID recurringInvoiceId = UUID.randomUUID();
        UpdateRecurringInvoiceRequest request = UpdateRecurringInvoiceRequest.builder()
                .paymentTermsDays(45)
                .build();

        RecurringInvoice recurringInvoice = recurringInvoice(recurringInvoiceId);
        recurringInvoice.setPaymentTermsDays(45);

        when(recurringInvoiceService.update(eq(recurringInvoiceId), any(UpdateRecurringInvoiceRequest.class)))
                .thenReturn(recurringInvoice);

        mockMvc.perform(patch("/api/v1/recurring-invoices/{id}", recurringInvoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTermsDays").value(45));
    }

    @Test
    void deactivateRecurringInvoice_returnsUpdatedResponse() throws Exception {
        UUID recurringInvoiceId = UUID.randomUUID();
        RecurringInvoice recurringInvoice = recurringInvoice(recurringInvoiceId);
        recurringInvoice.setActive(false);

        when(recurringInvoiceService.deactivate(recurringInvoiceId)).thenReturn(recurringInvoice);

        mockMvc.perform(patch("/api/v1/recurring-invoices/{id}/deactivate", recurringInvoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
