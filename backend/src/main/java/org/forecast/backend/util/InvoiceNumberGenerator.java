package org.forecast.backend.util;

import lombok.RequiredArgsConstructor;
import org.forecast.backend.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Utility class to generate unique invoice numbers following a pattern:
 * Format: INV-YYYY-NNNNN (e.g., INV-2026-00001)
 * - INV: Prefix
 * - YYYY: Current year
 * - NNNNN: Sequential number (5 digits with leading zeros)
 */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    /**
     * Generates a unique invoice number following the pattern: INV-YYYY-NNNNN
     *
     * @return A formatted invoice number string
     */
    public String generateInvoiceNumber() {
        long seq = invoiceRepository.getNextInvoiceSequence();
        String year = String.valueOf(Year.now().getValue());
        return String.format("INV-%s-%05d", year, seq);
    }
}

