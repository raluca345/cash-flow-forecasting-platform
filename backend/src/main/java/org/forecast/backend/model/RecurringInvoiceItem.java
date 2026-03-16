package org.forecast.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recurring_invoice_items")
@Getter
@Setter
public class RecurringInvoiceItem extends AbstractLineItem {

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "recurring_invoice_id", nullable = false)
    private RecurringInvoice recurringInvoice;
}
