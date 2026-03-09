package org.forecast.backend.dtos.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.model.InvoiceItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponse {

    private UUID id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    /** Line amount excluding VAT: unitPrice * quantity */
    private BigDecimal netAmount;

    private BigDecimal vatRatePercent;
    private BigDecimal vatAmount;

    /** Line amount including VAT: netAmount + vatAmount */
    private BigDecimal grossAmount;

    public static InvoiceItemResponse fromEntity(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .vatRatePercent(item.getVatRatePercent())
                .vatAmount(item.getVatAmount())
                .netAmount(item.getNetAmount())
                .grossAmount(item.getGrossAmount())
                .build();
    }

    public static List<InvoiceItemResponse> fromEntities(List<InvoiceItem> items) {
        List<InvoiceItemResponse> out = new ArrayList<>();
        if (items == null) return out;
        for (InvoiceItem item : items) {
            out.add(fromEntity(item));
        }
        return out;
    }
}

