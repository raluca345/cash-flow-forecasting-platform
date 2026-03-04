package org.forecast.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.CreateInvoiceRequest;
import org.forecast.backend.dtos.InvoiceResponse;
import org.forecast.backend.dtos.PaginatedResponse;
import org.forecast.backend.dtos.UpdateInvoiceDraftPartialRequest;
import org.forecast.backend.model.Invoice;
import org.forecast.backend.service.InvoiceService;
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

    private final InvoiceService invoiceService;

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
            @PageableDefault(size = 10, page = 1, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Invoice> invoicePage = invoiceService.getAllInvoices(pageable);
        PaginatedResponse<InvoiceResponse> response = PaginatedResponse.fromPage(invoicePage.map(InvoiceResponse::fromEntity));
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping(path = "{invoiceNumber}/send", consumes = "application/json", produces = "application/json")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.sendInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @PatchMapping(path = "{invoiceNumber}/pay", consumes = "application/json", produces = "application/json")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.payInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<Void> deleteInvoiceDraft(@PathVariable String invoiceNumber) {
        invoiceService.deleteInvoice(invoiceNumber);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "{invoiceNumber}/cancel", consumes = "application/json", produces = "application/json")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.cancelInvoice(invoiceNumber);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }

    @PatchMapping(path = "{invoiceNumber}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<InvoiceResponse> editInvoice(@PathVariable String invoiceNumber,
                                                       @Valid @RequestBody UpdateInvoiceDraftPartialRequest request) {
        Invoice invoice = invoiceService.editInvoice(invoiceNumber, request);
        return ResponseEntity.ok().body(InvoiceResponse.fromEntity(invoice));
    }
}

