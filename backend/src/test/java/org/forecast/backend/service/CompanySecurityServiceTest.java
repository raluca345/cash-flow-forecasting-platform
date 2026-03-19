package org.forecast.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.forecast.backend.config.JwtAuthenticationDetails;
import org.forecast.backend.enums.Role;
import org.forecast.backend.model.Company;
import org.forecast.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

class CompanySecurityServiceTest {

    private final CompanySecurityService companySecurityService = new CompanySecurityService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assertCompanyAccess_allowsMatchingTenantCompany() {
        UUID companyId = UUID.randomUUID();
        setAuthentication(user(Role.FINANCE, companyId), companyId);

        assertDoesNotThrow(() -> companySecurityService.assertCompanyAccess(companyId, "Access denied."));
    }

    @Test
    void assertCompanyAccess_deniesDifferentTenantCompany() {
        UUID authenticatedCompanyId = UUID.randomUUID();
        UUID requestedCompanyId = UUID.randomUUID();
        setAuthentication(user(Role.COMPANY_ADMIN, authenticatedCompanyId), authenticatedCompanyId);

        assertThrows(
                AccessDeniedException.class,
                () -> companySecurityService.assertCompanyAccess(requestedCompanyId, "Access denied."));
    }

    @Test
    void assertCompanyAccess_allowsSystemAdminWithoutCompanyContext() {
        UUID requestedCompanyId = UUID.randomUUID();
        setAuthentication(user(Role.SYSTEM_ADMIN, null), null);

        assertDoesNotThrow(() -> companySecurityService.assertCompanyAccess(requestedCompanyId, "Access denied."));
    }

    private static void setAuthentication(User user, UUID companyId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new JwtAuthenticationDetails(companyId));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static User user(Role role, UUID companyId) {
        User user = new User();
        user.setRole(role);

        if (companyId != null) {
            Company company = new Company();
            company.setId(companyId);
            user.setCompany(company);
        }

        return user;
    }
}
