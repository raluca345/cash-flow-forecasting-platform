package org.forecast.backend.dtos.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.enums.LineItemType;
import org.forecast.backend.model.AbstractLineItem;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemResponse {

    private UUID id;
    private String description;
    private LineItemType type;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal netAmount;
    private BigDecimal vatRatePercent;
    private BigDecimal vatAmount;
    private BigDecimal grossAmount;

    public static LineItemResponse fromEntity(AbstractLineItem item) {
        return LineItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .type(item.getType())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .netAmount(item.getNetAmount())
                .vatRatePercent(item.getVatRatePercent())
                .vatAmount(item.getVatAmount())
                .grossAmount(item.getGrossAmount())
                .build();
    }

    public static List<LineItemResponse> fromEntities(List<? extends AbstractLineItem> items) {
        List<LineItemResponse> responses = new ArrayList<>();
        if (items == null) {
            return responses;
        }
        for (AbstractLineItem item : items) {
            responses.add(fromEntity(item));
        }
        return responses;
    }
}
