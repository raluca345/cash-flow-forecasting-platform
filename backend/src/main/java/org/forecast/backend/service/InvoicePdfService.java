package org.forecast.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.pdf.InvoicePdfRenderer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final IInvoiceService invoiceService;
    private final InvoicePdfRenderer invoicePdfRenderer;

    public byte[] generatePdf(String invoiceNumber, HttpServletRequest request) {
        Invoice invoice = invoiceService.getInvoiceForPdf(invoiceNumber);
        String baseUri = resolveBaseUri(request);
        return invoicePdfRenderer.renderPdfBytes(invoice, baseUri);
    }

    public String generateHtml(String invoiceNumber) {
        Invoice invoice = invoiceService.getInvoiceForPdf(invoiceNumber);
        return invoicePdfRenderer.renderHtml(invoice);
    }

    private String resolveBaseUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);
        return defaultPort
                ? (scheme + "://" + host)
                : (scheme + "://" + host + ":" + port);
    }
}

