package org.forecast.backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceDraftPartialRequest {

    @DecimalMin(value = "0.00", message = "Tax rate must be >= 0")
    @DecimalMax(value = "100.00", message = "Tax rate must be <= 100")
    @Digits(integer = 3, fraction = 3, message = "Tax rate must have at most 3 digits and 3 decimal places")
    private BigDecimal taxRatePercent;

    @Valid
    private List<CreateInvoiceItemRequest> items;

    @PastOrPresent(message = "Issue date cannot be in the future")
    private LocalDate issueDate;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;
}
