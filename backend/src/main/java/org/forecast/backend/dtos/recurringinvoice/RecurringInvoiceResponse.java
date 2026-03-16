package org.forecast.backend.dtos.recurringinvoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.dtos.shared.LineItemResponse;
import org.forecast.backend.enums.RecurringInvoiceFrequency;
import org.forecast.backend.model.RecurringInvoice;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceResponse {

    private UUID id;
    private UUID clientId;
    private String clientName;
    private UUID companyId;
    private List<LineItemResponse> items;
    private BigDecimal netTotal;
    private BigDecimal vatTotal;
    private BigDecimal grossTotal;
    private String currency;
    private RecurringInvoiceFrequency frequency;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextGenerationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastGeneratedAt;

    private int paymentTermsDays;
    private boolean draft;
    private boolean active;

    public static RecurringInvoiceResponse fromEntity(RecurringInvoice recurringInvoice) {
        return RecurringInvoiceResponse.builder()
                .id(recurringInvoice.getId())
                .clientId(recurringInvoice.getClient().getId())
                .clientName(recurringInvoice.getClient().getName())
                .companyId(recurringInvoice.getCompany().getId())
                .items(LineItemResponse.fromEntities(recurringInvoice.getItems()))
                .netTotal(recurringInvoice.getNetTotal())
                .vatTotal(recurringInvoice.getVatTotal())
                .grossTotal(recurringInvoice.getGrossTotal())
                .currency(recurringInvoice.getCurrency())
                .frequency(recurringInvoice.getFrequency())
                .startDate(recurringInvoice.getStartDate())
                .endDate(recurringInvoice.getEndDate())
                .nextGenerationDate(recurringInvoice.getNextGenerationDate())
                .lastGeneratedAt(recurringInvoice.getLastGeneratedAt())
                .paymentTermsDays(recurringInvoice.getPaymentTermsDays())
                .draft(recurringInvoice.isDraft())
                .active(recurringInvoice.isActive())
                .build();
    }

    public static List<RecurringInvoiceResponse> fromEntities(List<RecurringInvoice> recurringInvoices) {
        return recurringInvoices.stream()
                .map(RecurringInvoiceResponse::fromEntity)
                .toList();
    }
}
