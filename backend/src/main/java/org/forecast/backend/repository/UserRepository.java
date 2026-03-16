package org.forecast.backend.repository;

import org.forecast.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    List<User> findByCompanyId(UUID companyId);

    Page<User> findByCompanyId(UUID companyId, Pageable pageable);
}

