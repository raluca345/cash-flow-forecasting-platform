package org.forecast.backend.model;

import jakarta.persistence.*;
import org.forecast.backend.enums.RecurringInvoiceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurring_invoices")
public class RecurringInvoice extends BaseEntity{

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, precision=15, scale=2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringInvoiceFrequency frequency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate nextGenerationDate;

    @Column(nullable = false)
    private boolean active = true;

    public Invoice generateInvoice(String invoiceNumber){

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setClient(client);
        invoice.setAmount(amount);

        // Default currency assumptions for recurring invoices (can be made configurable later)
        invoice.setCurrency("USD");
        invoice.setExchangeRate(java.math.BigDecimal.ONE);
        invoice.setAmountBaseCurrency(amount);

        invoice.setIssueDate(nextGenerationDate);
        invoice.setDueDate(nextGenerationDate.plusDays(30)); //configurable later

        return invoice;
    }

    public void advanceNextGenerationDate() {
        switch (frequency) {
            case WEEKLY -> nextGenerationDate = nextGenerationDate.plusWeeks(1);
            case MONTHLY -> nextGenerationDate = nextGenerationDate.plusMonths(1);
            case QUARTERLY -> nextGenerationDate = nextGenerationDate.plusMonths(3);
            case YEARLY -> nextGenerationDate = nextGenerationDate.plusYears(1);
        }
    }
}
