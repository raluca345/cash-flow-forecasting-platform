package org.forecast.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Getter
@Setter
public class InvoiceItem {
    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @DecimalMin(value = "0.00", message = "Quantity must be >= 0")
    @Digits(integer = 13, fraction = 3, message = "Quantity must have at most 13 digits and 3 decimal places")
    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Line net amount excluding VAT: unitPrice * quantity
     */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(precision = 15, scale = 2)
    private BigDecimal netAmount;

    @NotNull
    @DecimalMin(value = "0.00", message = "VAT rate must be >= 0")
    @DecimalMax(value = "100.00", message = "VAT rate must be <= 100")
    @Digits(integer = 3, fraction = 3, message = "VAT rate must have at most 3 digits and 3 decimal places")
    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal vatRatePercent = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "VAT amount must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "VAT amount must have at most 13 digits and 2 decimal places")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /**
     * Line gross amount including VAT.
     */
    @NotNull
    @DecimalMin(value = "0.00", message = "Subtotal must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "Subtotal must have at most 13 digits and 2 decimal places")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    void computeTotals() {
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal up = unitPrice == null ? BigDecimal.ZERO : unitPrice;

        // net line total
        this.netAmount = up.multiply(q).setScale(2, RoundingMode.HALF_UP);

        BigDecimal rate = this.vatRatePercent;
        if (rate == null) rate = BigDecimal.ZERO;

        this.vatAmount = this.netAmount
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        this.grossAmount = this.netAmount.add(this.vatAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
