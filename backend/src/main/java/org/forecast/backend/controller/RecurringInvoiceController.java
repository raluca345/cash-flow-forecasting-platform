package org.forecast.backend.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.recurringinvoice.CreateRecurringInvoiceRequest;
import org.forecast.backend.dtos.recurringinvoice.RecurringInvoiceResponse;
import org.forecast.backend.dtos.recurringinvoice.UpdateRecurringInvoiceRequest;
import org.forecast.backend.dtos.shared.PaginatedResponse;
import org.forecast.backend.model.RecurringInvoice;
import org.forecast.backend.service.IRecurringInvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recurring-invoices")
@RequiredArgsConstructor
public class RecurringInvoiceController {

    private final IRecurringInvoiceService recurringInvoiceService;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<RecurringInvoiceResponse> createRecurringInvoice(
            @Valid @RequestBody CreateRecurringInvoiceRequest request
    ) {
        RecurringInvoice recurringInvoice = recurringInvoiceService.create(request);
        return ResponseEntity.ok(RecurringInvoiceResponse.fromEntity(recurringInvoice));
    }

    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<PaginatedResponse<RecurringInvoiceResponse>> listRecurringInvoices(
            @PageableDefault(size = 10, sort = "nextGenerationDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RecurringInvoice> recurringInvoicePage = recurringInvoiceService.listAll(pageable);
        return ResponseEntity.ok(
                PaginatedResponse.fromPageContent(
                        recurringInvoicePage,
                        RecurringInvoiceResponse.fromEntities(recurringInvoicePage.getContent())
                )
        );
    }

    @GetMapping("/{recurringInvoiceId}")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<RecurringInvoiceResponse> getRecurringInvoice(@PathVariable UUID recurringInvoiceId) {
        RecurringInvoice recurringInvoice = recurringInvoiceService.get(recurringInvoiceId);
        return ResponseEntity.ok(RecurringInvoiceResponse.fromEntity(recurringInvoice));
    }

    @PatchMapping("/{recurringInvoiceId}")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<RecurringInvoiceResponse> updateRecurringInvoice(
            @PathVariable UUID recurringInvoiceId,
            @Valid @RequestBody UpdateRecurringInvoiceRequest request
    ) {
        RecurringInvoice recurringInvoice = recurringInvoiceService.update(recurringInvoiceId, request);
        return ResponseEntity.ok(RecurringInvoiceResponse.fromEntity(recurringInvoice));
    }

    @PatchMapping("/{recurringInvoiceId}/activate")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<RecurringInvoiceResponse> activateRecurringInvoice(@PathVariable UUID recurringInvoiceId) {
        RecurringInvoice recurringInvoice = recurringInvoiceService.activate(recurringInvoiceId);
        return ResponseEntity.ok(RecurringInvoiceResponse.fromEntity(recurringInvoice));
    }

    @PatchMapping("/{recurringInvoiceId}/deactivate")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<RecurringInvoiceResponse> deactivateRecurringInvoice(@PathVariable UUID recurringInvoiceId) {
        RecurringInvoice recurringInvoice = recurringInvoiceService.deactivate(recurringInvoiceId);
        return ResponseEntity.ok(RecurringInvoiceResponse.fromEntity(recurringInvoice));
    }
}
