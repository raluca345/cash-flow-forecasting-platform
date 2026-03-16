package org.forecast.backend.service;

import org.forecast.backend.dtos.invoice.CreateInvoiceRequest;
import org.forecast.backend.dtos.invoice.InvoiceSearchCriteria;
import org.forecast.backend.dtos.invoice.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IInvoiceService {
    Invoice createInvoice(CreateInvoiceRequest request);

    Invoice getInvoice(String invoiceNumber);


    Invoice getInvoiceForPdf(String invoiceNumber);

    Page<Invoice> getAllInvoices(Pageable pageable);

    void deleteInvoice(String invoiceNumber);

    Invoice sendInvoice(String invoiceNumber);

    Invoice payInvoice(String invoiceNumber);

    Invoice cancelInvoice(String invoiceNumber);

    Invoice editInvoice(String invoiceNumber, UpdateInvoiceDraftPartialRequest request);

    void markOverdueInvoices();

    Page<Invoice> filterByCriteria(InvoiceSearchCriteria criteria, Pageable pageable);
}
