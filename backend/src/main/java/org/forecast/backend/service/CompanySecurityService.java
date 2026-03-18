package org.forecast.backend.service;

import java.util.UUID;
import org.forecast.backend.config.JwtAuthenticationDetails;
import org.forecast.backend.enums.Role;
import org.forecast.backend.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CompanySecurityService {

    /**
     * Return the company id associated with the currently authenticated user, or null if not authenticated.
     */
    private UUID getCurrentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        if (auth.getDetails() instanceof JwtAuthenticationDetails details && details.companyId() != null) {
            return details.companyId();
        }

        if (auth.getPrincipal() instanceof User user && user.getCompany() != null) {
            return user.getCompany().getId();
        }

        return null;
    }

    public Role getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        if (auth.getPrincipal() instanceof User user && user.getRole() != null) {
            return user.getRole();
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(value.substring("ROLE_".length()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore authorities outside the application's role model.
                }
            }
        }

        return null;
    }

    public boolean isSystemAdmin() {
        return getCurrentRole() == Role.SYSTEM_ADMIN;
    }

    public UUID requireCurrentCompanyId(String message) {
        UUID companyId = getCurrentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException(message);
        }
        return companyId;
    }

    public void requireSystemAdmin(String message) {
        if (!isSystemAdmin()) {
            throw new AccessDeniedException(message);
        }
    }

    public void requireCompanyAdmin(String message) {
        if (getCurrentRole() != Role.COMPANY_ADMIN) {
            throw new AccessDeniedException(message);
        }
    }

    public void assertCompanyAccess(UUID companyId, String message) {
        if (companyId == null) {
            throw new AccessDeniedException(message);
        }
        if (isSystemAdmin()) {
            return;
        }

        UUID currentCompanyId = requireCurrentCompanyId(message);
        if (!currentCompanyId.equals(companyId)) {
            throw new AccessDeniedException(message);
        }
    }

}

