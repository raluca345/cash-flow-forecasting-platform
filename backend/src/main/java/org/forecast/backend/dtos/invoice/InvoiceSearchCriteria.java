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
}

