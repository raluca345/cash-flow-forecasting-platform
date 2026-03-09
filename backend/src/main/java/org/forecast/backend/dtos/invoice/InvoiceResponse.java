package org.forecast.backend.dtos.invoice;

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

    /** Sum of item net amounts (excluding VAT) */
    private BigDecimal netTotal;

    /** Total VAT amount (sum of item vatAmount) */
    private BigDecimal vatTotal;

    /** VAT-inclusive total (netTotal + vatTotal) */
    private BigDecimal grossTotal;

    private String currency;

    private List<InvoiceItemResponse> items;

    private BigDecimal amountBaseCurrency;
    private BigDecimal exchangeRate;

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
                .netTotal(invoice.getNetTotal())
                .vatTotal(invoice.getVatTotal())
                .grossTotal(invoice.getGrossTotal())
                .currency(invoice.getCurrency())
                .items(invoice.getItems() == null ? List.of() : InvoiceItemResponse.fromEntities(invoice.getItems()))
                .amountBaseCurrency(invoice.getAmountBaseCurrency())
                .exchangeRate(invoice.getExchangeRate())
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

