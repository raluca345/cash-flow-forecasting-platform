package org.forecast.backend.dtos.invoice;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0.00", message = "Quantity must be >= 0")
    @Digits(integer = 13, fraction = 3, message = "Quantity must have at most 13 digits and 3 decimal places")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Unit price must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "Unit price must have at most 13 digits and 2 decimal places")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "VAT rate must be >= 0")
    @DecimalMax(value = "100.00", message = "VAT rate must be <= 100")
    @Digits(integer = 3, fraction = 3, message = "VAT rate must have at most 3 digits and 3 decimal places")
    private BigDecimal vatRatePercent;
}

