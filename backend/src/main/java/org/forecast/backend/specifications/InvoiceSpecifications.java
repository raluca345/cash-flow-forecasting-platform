package org.forecast.backend.specifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.forecast.backend.enums.InvoiceStatus;
import org.forecast.backend.model.Invoice;
import org.springframework.data.jpa.domain.Specification;

public class InvoiceSpecifications {

  public static Specification<Invoice> notDeleted() {
    return (root, query, cb) -> cb.isFalse(root.get("deleted"));
  }

  public static Specification<Invoice> hasStatus(InvoiceStatus status) {
    return (root, query, cb) ->
        status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
  }

  public static Specification<Invoice> hasClientId(UUID clientId) {
    return (root, query, cb) ->
        clientId == null ? cb.conjunction() : cb.equal(root.get("client").get("id"), clientId);
  }

  public static Specification<Invoice> clientNameContains(String clientName) {
    return (root, query, cb) ->
        clientName == null
            ? cb.conjunction()
            : cb.like(
                cb.lower(root.get("client").get("name")), "%" + clientName.toLowerCase() + "%");
  }

  public static Specification<Invoice> hasCompanyId(UUID companyId) {
    return (root, query, cb) -> companyId == null ? cb.conjunction() : cb.equal(root.get("company").get("id"), companyId);
  }

  public static Specification<Invoice> invoiceNumberContains(String invoiceNumber) {
    return (root, query, cb) ->
        invoiceNumber == null
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("invoiceNumber")), "%" + invoiceNumber.toLowerCase() + "%");
  }

  public static Specification<Invoice> amountGreaterThan(BigDecimal minAmount) {
    return (root, query, cb) ->
        minAmount == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("grossTotal"), minAmount);
  }

  public static Specification<Invoice> amountLessThan(BigDecimal maxAmount) {
    return (root, query, cb) ->
        maxAmount == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("grossTotal"), maxAmount);
  }

  public static Specification<Invoice> dueDateFrom(LocalDate dueDate) {
    return (root, query, cb) ->
        dueDate == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("dueDate"), dueDate);
  }

  public static Specification<Invoice> dueDateTo(LocalDate dueDate) {
    return (root, query, cb) ->
        dueDate == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("dueDate"), dueDate);
  }

  public static Specification<Invoice> issueDateFrom(LocalDate issueDate) {
    return (root, query, cb) ->
        issueDate == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("issueDate"), issueDate);
  }

  public static Specification<Invoice> issueDateTo(LocalDate issueDate) {
    return (root, query, cb) ->
        issueDate == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("issueDate"), issueDate);
  }

  public static Specification<Invoice> isOverdue(Boolean isOverdue) {
    return (root, query, cb) -> {
      if (isOverdue == null || !isOverdue) return cb.conjunction();
      return cb.equal(root.get("status"), InvoiceStatus.OVERDUE);
    };
  }

  public static Specification<Invoice> isUnpaid(Boolean isUnpaid) {
    return (root, query, cb) -> {
      if (isUnpaid == null || !isUnpaid) return cb.conjunction();
      return cb.or(
              cb.equal(root.get("status"), InvoiceStatus.SENT),
              cb.equal(root.get("status"), InvoiceStatus.OVERDUE)
      );
    };
  }

  public static Specification<Invoice> sentAtFrom(Instant sentAt) {
    return (root, query, cb) ->
        sentAt == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("sentAt"), sentAt);
  }

  public static Specification<Invoice> sentAtTo(Instant sentAt) {
    return (root, query, cb) ->
        sentAt == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("sentAt"), sentAt);
  }

  public static Specification<Invoice> paidAtFrom(Instant paidAt) {
    return (root, query, cb) ->
        paidAt == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("paidAt"), paidAt);
  }

  public static Specification<Invoice> paidAtTo(Instant paidAt) {
    return (root, query, cb) ->
        paidAt == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("paidAt"), paidAt);
  }

  public static Specification<Invoice> hasCurrency(String currency) {
    return (root, query, cb) -> {
      if (currency == null || currency.isBlank()) return cb.conjunction();
      return cb.equal(cb.upper(root.get("currency")), currency.trim().toUpperCase());
    };
  }
}
