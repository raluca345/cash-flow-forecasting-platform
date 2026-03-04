package org.forecast.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.forecast.backend.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"invoiceNumber"})
        }
)
@Getter
@Setter
public class Invoice extends BaseEntity{
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "Invoice number is required")
    @Column(nullable = false)
    private String invoiceNumber;

    @NotNull(message = "Client is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits and 2 decimal places")
    @Column(nullable = false, precision=15, scale=2)
    private BigDecimal amount;

    @NotNull(message = "Issue date is required")
    @PastOrPresent(message = "Issue date cannot be in the future")
    @Column(nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    @Column(nullable = false)
    private LocalDate dueDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private Instant sentAt;

    private Instant paidAt;

    @Column(nullable = false)
    private boolean deleted = false;

    public void markAsSent() {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be sent");
        }

        status = InvoiceStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsPaid(Instant paidAt) {
        if (status != InvoiceStatus.SENT && status != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException("Only sent or overdue invoices can be paid");
        }

        status = InvoiceStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markOverdue(LocalDate today) {
        if (status == InvoiceStatus.SENT && dueDate.isBefore(today)) {
            status = InvoiceStatus.OVERDUE;
        }
    }

    public void cancel() {
        if (status == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }
        this.status = InvoiceStatus.CANCELLED;
    }

    public boolean isOverdue(LocalDate today) {
        return status == InvoiceStatus.SENT && dueDate.isBefore(today);
    }

    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

}
