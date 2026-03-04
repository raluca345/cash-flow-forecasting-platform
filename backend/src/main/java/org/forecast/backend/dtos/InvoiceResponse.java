package org.forecast.backend.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.model.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private String clientName;
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private InvoiceStatus status;

    private Instant sentAt;

    private Instant paidAt;

    public static InvoiceResponse fromEntity(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .clientName(invoice.getClient().getName())
                .amount(invoice.getAmount())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .sentAt(invoice.getSentAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }

    public static List<InvoiceResponse> fromEntities(List<Invoice> invoices) {
        return invoices.stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }
}

