package org.forecast.backend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceItemsRelationshipTest {

    @Test
    void addItem_setsBackReference() {
        Invoice invoice = new Invoice();
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(new BigDecimal("2.000"));
        item.setUnitPrice(new BigDecimal("10.00"));

        invoice.addItem(item);

        assertEquals(1, invoice.getItems().size());
        assertSame(invoice, item.getInvoice());
    }

    @Test
    void removeItem_clearsBackReference() {
        Invoice invoice = new Invoice();
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(new BigDecimal("1.000"));
        item.setUnitPrice(new BigDecimal("10.00"));

        invoice.addItem(item);
        invoice.removeItem(item);

        assertTrue(invoice.getItems().isEmpty());
        assertNull(item.getInvoice());
    }

    @Test
    void recalculateAmountFromItems_sumsItemTotals() {
        Invoice invoice = new Invoice();

        InvoiceItem item1 = new InvoiceItem();
        item1.setQuantity(new BigDecimal("2.000"));
        item1.setUnitPrice(new BigDecimal("10.00"));
        item1.setVatRatePercent(BigDecimal.ZERO);
        item1.setNetAmount(new BigDecimal("20.00"));
        item1.setVatAmount(new BigDecimal("0.00"));
        item1.setGrossAmount(new BigDecimal("20.00"));

        InvoiceItem item2 = new InvoiceItem();
        item2.setQuantity(new BigDecimal("1.000"));
        item2.setUnitPrice(new BigDecimal("5.50"));
        item2.setVatRatePercent(BigDecimal.ZERO);
        item2.setNetAmount(new BigDecimal("5.50"));
        item2.setVatAmount(new BigDecimal("0.00"));
        item2.setGrossAmount(new BigDecimal("5.50"));

        invoice.addItem(item1);
        invoice.addItem(item2);

        invoice.recalculateAmountFromItems();

        assertEquals(new BigDecimal("25.50"), invoice.getNetTotal());
        assertEquals(new BigDecimal("0.00"), invoice.getVatTotal());
        assertEquals(new BigDecimal("25.50"), invoice.getGrossTotal());
    }
}
