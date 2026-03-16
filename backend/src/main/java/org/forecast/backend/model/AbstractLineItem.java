package org.forecast.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.forecast.backend.enums.LineItemType;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractLineItem {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LineItemType type = LineItemType.OTHER;

    @NotNull
    @DecimalMin(value = "0.00", message = "Quantity must be >= 0")
    @Digits(integer = 13, fraction = 3, message = "Quantity must have at most 13 digits and 3 decimal places")
    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
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

    @NotNull
    @DecimalMin(value = "0.00", message = "Gross amount must be >= 0")
    @Digits(integer = 13, fraction = 2, message = "Gross amount must have at most 13 digits and 2 decimal places")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    public void computeTotals() {
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal up = unitPrice == null ? BigDecimal.ZERO : unitPrice;

        this.netAmount = up.multiply(q).setScale(2, RoundingMode.HALF_UP);

        BigDecimal rate = vatRatePercent == null ? BigDecimal.ZERO : vatRatePercent;
        this.vatAmount = this.netAmount
                .multiply(rate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        this.grossAmount = this.netAmount.add(this.vatAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
