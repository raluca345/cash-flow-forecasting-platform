package org.forecast.backend.dtos.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.forecast.backend.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class InvoiceSearchCriteria {

    private InvoiceStatus status;

    private UUID clientId;

    private String clientName;

    private String invoiceNumber;

    private String currency;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDateTo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDateTo;

    private Boolean overdue;

    private Boolean unpaid;

    private Instant sentAtFrom;

    private Instant sentAtTo;

    private Instant paidAtFrom;

    private Instant paidAtTo;

    public void validateRanges() {
        validateRange(minAmount, maxAmount, "Minimum amount cannot be greater than maximum amount");
        validateRange(dueDateFrom, dueDateTo, "Due date from cannot be after due date to");
        validateRange(issueDateFrom, issueDateTo, "Issue date from cannot be after issue date to");
        validateRange(sentAtFrom, sentAtTo, "Sent-at from cannot be after sent-at to");
        validateRange(paidAtFrom, paidAtTo, "Paid-at from cannot be after paid-at to");
    }

    private static <T extends Comparable<? super T>> void validateRange(T from, T to, String message) {
        if (from != null && to != null && from.compareTo(to) > 0) {
            throw new IllegalArgumentException(message);
        }
    }
}

