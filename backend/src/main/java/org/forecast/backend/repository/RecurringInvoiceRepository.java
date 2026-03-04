package org.forecast.backend.repository;

import jakarta.persistence.LockModeType;
import org.forecast.backend.model.RecurringInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecurringInvoiceRepository extends JpaRepository<RecurringInvoice, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecurringInvoice r WHERE r.active = true AND r.nextGenerationDate <= :today")
    List<RecurringInvoice> findDueWithLock(@Param("today") LocalDate today);
}
