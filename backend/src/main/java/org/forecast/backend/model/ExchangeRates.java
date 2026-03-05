package org.forecast.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "exchange_rates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fx_from_to", columnNames = {"from_currency", "to_currency"})
        },
        indexes = {
                @Index(name = "idx_fx_from_to", columnList = "from_currency,to_currency"),
                @Index(name = "idx_fx_date_from_to", columnList = "date,from_currency,to_currency")
        }
)
@Getter
@Setter
public class ExchangeRates {
    @Id
    @GeneratedValue
    private UUID id;

    @NotNull(message = "Rate date is required")
    @Column(nullable = false)
    private Instant date;

    @NotBlank(message = "From currency is required")
    @Size(min = 3, max = 3, message = "From currency must be a 3-letter ISO code")
    @Pattern(regexp = "[A-Z]{3}", message = "From currency must be uppercase 3 letters (e.g. EUR)")
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    @Size(min = 3, max = 3, message = "To currency must be a 3-letter ISO code")
    @Pattern(regexp = "[A-Z]{3}", message = "To currency must be uppercase 3 letters (e.g. USD)")
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be greater than 0")
    @Digits(integer = 10, fraction = 6, message = "Rate must have at most 10 digits and 6 decimal places")
    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal rate;
}
