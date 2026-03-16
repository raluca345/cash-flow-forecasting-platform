package org.forecast.backend.service;

import java.util.UUID;
import org.forecast.backend.config.JwtAuthenticationDetails;
import org.forecast.backend.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CompanySecurityService {

    /**
     * Return the company id associated with the currently authenticated user, or null if not authenticated.
     */
    public UUID getCurrentCompanyId() {
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

    public UUID requireCurrentCompanyId(String message) {
        UUID companyId = getCurrentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException(message);
        }
        return companyId;
    }
}

