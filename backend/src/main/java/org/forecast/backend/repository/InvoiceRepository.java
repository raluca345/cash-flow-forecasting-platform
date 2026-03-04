package org.forecast.backend.repository;

import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByDeletedFalse(Pageable pageable);

    List<Invoice> findByStatusAndDeletedFalse(InvoiceStatus status, Pageable pageable);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByInvoiceNumberAndDeletedFalse(String invoiceNumber);

    List<Invoice> findByDeletedFalseOrderByIssueDateDesc();

    Page<Invoice> findByDeletedFalseOrderByIssueDateDesc(Pageable pageable);

    Page<Invoice> findByStatusInAndDueDateBeforeAndDeletedFalse(List<InvoiceStatus> statuses, LocalDate dueDate, Pageable pageable);

    @Query(value = "SELECT nextval('invoice_sequence')", nativeQuery = true)
    long getNextInvoiceSequence();
}
