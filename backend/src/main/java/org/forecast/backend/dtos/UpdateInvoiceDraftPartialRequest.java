package org.forecast.backend.dtos;

import jakarta.validation.Valid;
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

    @DecimalMin(value = "0.00", message = "Tax amount must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "Tax amount must have at most 13 digits and 2 decimal places")
    private BigDecimal taxAmount;

    /**
     * If present, replaces the current item list.
     */
    @Valid
    private List<CreateInvoiceItemRequest> items;

    @PastOrPresent(message = "Issue date cannot be in the future")
    private LocalDate issueDate;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;
}
