package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.*;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.service.IInvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest createInvoiceRequest) {
        Invoice invoice = invoiceService.createInvoice(createInvoiceRequest);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.getInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<InvoiceResponse>> getAllInvoices(
            @PageableDefault(size = 10, page = 0, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Invoice> invoicePage = invoiceService.getAllInvoices(pageable);
        PaginatedResponse<InvoiceResponse> response = PaginatedResponse.fromPageContent(
                invoicePage,
                InvoiceResponse.fromEntities(invoicePage.getContent())
        );
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("{invoiceNumber}/send")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.sendInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @PatchMapping("{invoiceNumber}/pay")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.payInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<Void> deleteInvoiceDraft(@PathVariable String invoiceNumber) {
        invoiceService.deleteInvoice(invoiceNumber);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("{invoiceNumber}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.cancelInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @PatchMapping("{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> editInvoice(@PathVariable String invoiceNumber,
                                                       @Valid @RequestBody UpdateInvoiceDraftPartialRequest request) {
        Invoice invoice = invoiceService.editInvoice(invoiceNumber, request);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<InvoiceResponse>> filterInvoices(
            @ModelAttribute InvoiceSearchCriteria criteria,
            @PageableDefault(size = 10, page = 0, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Invoice> filteredInvoicePage = invoiceService.filterByCriteria(criteria, pageable);
        PaginatedResponse<InvoiceResponse> response = PaginatedResponse.fromPageContent(
                filteredInvoicePage,
                InvoiceResponse.fromEntities(filteredInvoicePage.getContent())
        );
        return ResponseEntity.ok().body(response);
    }
}
