package org.forecast.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository
    extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

  Page<Invoice> findByCompanyIdAndDeletedFalseOrderByIssueDateDesc(
      UUID companyId, Pageable pageable);


  Page<Invoice> findByCompanyIdAndStatusInAndDueDateBeforeAndDeletedFalse(
      UUID companyId, List<InvoiceStatus> statuses, LocalDate dueDate, Pageable pageable);

  boolean existsByRecurringInvoiceIdAndIssueDate(UUID recurringInvoiceId, LocalDate issueDate);

  Optional<Invoice> findByInvoiceNumberAndCompanyIdAndDeletedFalse(
      String invoiceNumber, UUID companyId);

  @Query(value = "SELECT nextval('invoice_sequence')", nativeQuery = true)
  long getNextInvoiceSequence();

  @Query(
      "select distinct i from Invoice i "
          + "join fetch i.company "
          + "join fetch i.client "
          + "left join fetch i.items "
          + "where i.invoiceNumber = :invoiceNumber and i.company.id = :companyId and i.deleted = false")
  Optional<Invoice> findByInvoiceNumberForPdfAndCompanyId(
      String invoiceNumber, UUID companyId);
}
