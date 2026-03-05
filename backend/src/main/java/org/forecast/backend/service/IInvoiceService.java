package org.forecast.backend.service;

import org.forecast.backend.dtos.CreateInvoiceRequest;
import org.forecast.backend.dtos.InvoiceSearchCriteria;
import org.forecast.backend.dtos.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IInvoiceService {
    Invoice createInvoice(CreateInvoiceRequest request);

    Invoice getInvoice(String invoiceNumber);


    Page<Invoice> getAllInvoices(Pageable pageable);

    void deleteInvoice(String invoiceNumber);

    List<Invoice> getOpenInvoicesByStatus();

    List<Invoice> getOpenInvoicesByStatus(InvoiceStatus status);

    Invoice sendInvoice(String invoiceNumber);

    Invoice payInvoice(String invoiceNumber);

    Invoice cancelInvoice(String invoiceNumber);

    Invoice editInvoice(String invoiceNumber, UpdateInvoiceDraftPartialRequest request);

    void markOverdueInvoices();

    Page<Invoice> filterByCriteria(InvoiceSearchCriteria criteria, Pageable pageable);
}
