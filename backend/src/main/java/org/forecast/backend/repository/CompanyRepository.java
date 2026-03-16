package org.forecast.backend.repository;

import org.forecast.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
	Optional<Company> findByNameIgnoreCase(String name);
	Optional<Company> findByInviteCode(String inviteCode);

	// treat null expiry as non-expiring.
	@Query("select c from Company c where c.inviteCode = :code and (c.inviteCodeExpiresAt is null or c.inviteCodeExpiresAt > :now)")
	Optional<Company> findByInviteCodeAndNotExpired(@Param("code") String inviteCode, @Param("now") Instant now);
}

