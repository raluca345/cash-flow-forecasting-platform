package org.forecast.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.forecast.backend.enums.RecurringInvoiceFrequency;

@Entity
@Table(
        name = "recurring_invoices",
        indexes = {
                @Index(name = "idx_recurring_invoice_company", columnList = "company_id"),
                @Index(name = "idx_recurring_generation", columnList = "active, next_generation_date")
        }
)
@Getter
@Setter
public class RecurringInvoice extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(
            mappedBy = "recurringInvoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<RecurringInvoiceItem> items = new ArrayList<>();

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netTotal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal vatTotal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    @NotNull
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase 3 letters (e.g. USD)")
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringInvoiceFrequency frequency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate nextGenerationDate;

    private LocalDate lastGeneratedAt;

    @Column(nullable = false)
    private int paymentTermsDays = 30;

    @Column(nullable = false)
    private boolean draft = false;

    @Column(nullable = false)
    private boolean active = true;

    public void addItem(RecurringInvoiceItem item) {
        if (item == null) {
            return;
        }
        items.add(item);
        item.setRecurringInvoice(this);
    }

    public void removeItem(RecurringInvoiceItem item) {
        if (item == null) {
            return;
        }
        items.remove(item);
        item.setRecurringInvoice(null);
    }

    public void recalculateAmountFromItems() {
        BigDecimal computedNet = BigDecimal.ZERO;
        BigDecimal computedVat = BigDecimal.ZERO;

        if (items != null && !items.isEmpty()) {
            for (RecurringInvoiceItem item : items) {
                if (item == null) {
                    continue;
                }
                if (item.getNetAmount() == null || item.getVatAmount() == null || item.getGrossAmount() == null) {
                    item.computeTotals();
                }
                computedNet = computedNet.add(item.getNetAmount());
                computedVat = computedVat.add(item.getVatAmount());
            }
        }

        this.netTotal = computedNet.setScale(2, java.math.RoundingMode.HALF_UP);
        this.vatTotal = computedVat.setScale(2, java.math.RoundingMode.HALF_UP);
        this.grossTotal = this.netTotal.add(this.vatTotal).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void advanceNextGenerationDate() {
        LocalDate nextDate = switch (frequency) {
            case WEEKLY -> nextGenerationDate.plusWeeks(1);
            case MONTHLY -> nextGenerationDate.plusMonths(1);
            case QUARTERLY -> nextGenerationDate.plusMonths(3);
            case YEARLY -> nextGenerationDate.plusYears(1);
        };

        if (nextDate.isAfter(endDate)) {
            nextGenerationDate = nextDate;
            active = false;
            return;
        }

        nextGenerationDate = nextDate;
    }
}
