package org.forecast.backend.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.forecast.backend.model.RecurringInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringInvoiceRepository extends JpaRepository<RecurringInvoice, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT r FROM RecurringInvoice r " +
                    "WHERE r.active = true " +
                    "AND r.draft = false " +
                    "AND r.nextGenerationDate <= :today " +
                    "AND r.nextGenerationDate <= r.endDate"
    )
    Page<RecurringInvoice> findDueWithLock(@Param("today") LocalDate today, Pageable pageable);

    Optional<RecurringInvoice> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<RecurringInvoice> findByCompanyIdOrderByNextGenerationDateAsc(UUID companyId, Pageable pageable);
}
