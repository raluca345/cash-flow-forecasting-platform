package org.forecast.backend.dtos.recurringinvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.enums.RecurringInvoiceFrequency;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRecurringInvoiceRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<CreateInvoiceItemRequest> items;

    @NotNull(message = "Frequency is required")
    private RecurringInvoiceFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase 3 letters (e.g. USD)")
    private String currency;

    @Min(value = 1, message = "Payment terms days must be at least 1")
    @Builder.Default
    private Integer paymentTermsDays = 30;
}
