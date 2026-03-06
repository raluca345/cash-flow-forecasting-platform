package org.forecast.backend.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceItemRequest {

    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Long quantity;

    @DecimalMin(value = "0.00", message = "Unit price must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "Unit price must have at most 13 digits and 2 decimal places")
    private BigDecimal unitPrice;
}

