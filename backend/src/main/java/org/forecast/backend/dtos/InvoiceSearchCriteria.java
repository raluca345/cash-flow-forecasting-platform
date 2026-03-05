package org.forecast.backend.dtos;

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

    private String currency;

    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private Boolean overdue;
    private Boolean unpaid;

    private LocalDate issueDateFrom;
    private LocalDate issueDateTo;

    private String invoiceNumber;

    private Instant sentAtFrom;
    private Instant sentAtTo;

    private Instant paidAtFrom;
    private Instant paidAtTo;
}
