package org.forecast.backend.util;

import java.math.BigDecimal;
import org.forecast.backend.dtos.invoice.CreateInvoiceItemRequest;
import org.forecast.backend.enums.LineItemType;
import org.forecast.backend.model.AbstractLineItem;
import org.forecast.backend.model.InvoiceItem;
import org.forecast.backend.model.RecurringInvoiceItem;

public final class LineItemFactory {

    private LineItemFactory() {
    }

    public static InvoiceItem toInvoiceItem(CreateInvoiceItemRequest request) {
        return populate(new InvoiceItem(), request);
    }

    public static RecurringInvoiceItem toRecurringInvoiceItem(CreateInvoiceItemRequest request) {
        return populate(new RecurringInvoiceItem(), request);
    }

    public static InvoiceItem toInvoiceItem(RecurringInvoiceItem recurringInvoiceItem) {
        return populate(new InvoiceItem(), recurringInvoiceItem);
    }

    private static <T extends AbstractLineItem> T populate(T target, CreateInvoiceItemRequest request) {
        target.setDescription(request.getDescription());
        target.setType(request.getType() == null ? LineItemType.OTHER : request.getType());
        target.setQuantity(request.getQuantity());
        target.setUnitPrice(request.getUnitPrice());
        target.setVatRatePercent(request.getVatRatePercent() == null ? BigDecimal.ZERO : request.getVatRatePercent());
        return target;
    }

    private static <T extends AbstractLineItem> T populate(T target, AbstractLineItem source) {
        target.setDescription(source.getDescription());
        target.setType(source.getType() == null ? LineItemType.OTHER : source.getType());
        target.setQuantity(source.getQuantity());
        target.setUnitPrice(source.getUnitPrice());
        target.setVatRatePercent(source.getVatRatePercent() == null ? BigDecimal.ZERO : source.getVatRatePercent());
        return target;
    }
}
