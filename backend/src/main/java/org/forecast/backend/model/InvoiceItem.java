package org.forecast.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @PrePersist
    @PreUpdate
    void computeTotal() {
        if (unitPrice == null) return;
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        this.total = unitPrice.multiply(q);
    }
}
