package org.forecast.backend.dtos;

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
    private long quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;

    public static InvoiceItemResponse fromEntity(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .total(item.getTotal())
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

