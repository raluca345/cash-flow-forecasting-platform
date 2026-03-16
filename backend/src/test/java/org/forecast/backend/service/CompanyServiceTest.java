package org.forecast.backend.service;

import org.forecast.backend.model.Company;
import org.forecast.backend.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySecurityService companySecurityService;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void generateInviteCode_success_onFirstAttempt() {
        UUID id = UUID.randomUUID();
        Company c = new Company();
        c.setId(id);

        when(companySecurityService.getCurrentCompanyId()).thenReturn(id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));
        when(companyRepository.save(c)).thenAnswer(inv -> inv.getArgument(0));

        String code = companyService.generateInviteCode(id);
        assertThat(code).isNotNull();
        assertThat(code).hasSize(8);
        assertThat(c.getInviteCodeExpiresAt()).isNotNull();
    }

    @Test
    void generateInviteCode_retries_onCollision_thenSucceeds() {
        UUID id = UUID.randomUUID();
        Company c = new Company();
        c.setId(id);

        when(companySecurityService.getCurrentCompanyId()).thenReturn(id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));

        // First save throws DataIntegrityViolationException, second returns entity
        when(companyRepository.save(c))
                .thenThrow(new DataIntegrityViolationException("unique violation"))
                .thenAnswer(inv -> inv.getArgument(0));

        String code = companyService.generateInviteCode(id);
        assertThat(code).isNotNull();
        assertThat(code).hasSize(8);
        assertThat(c.getInviteCodeExpiresAt()).isNotNull();
    }

    @Test
    void generateInviteCode_fails_afterRetries() {
        UUID id = UUID.randomUUID();
        Company c = new Company();
        c.setId(id);

        when(companySecurityService.getCurrentCompanyId()).thenReturn(id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));

        // Always throw
        when(companyRepository.save(c)).thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThrows(IllegalStateException.class, () -> companyService.generateInviteCode(id));
    }

    @Test
    void getById_rejectsCrossCompanyAccess() {
        UUID requestedCompanyId = UUID.randomUUID();
        UUID currentCompanyId = UUID.randomUUID();

        Company c = new Company();
        c.setId(requestedCompanyId);

        when(companySecurityService.getCurrentCompanyId()).thenReturn(currentCompanyId);
        when(companyRepository.findById(requestedCompanyId)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> companyService.getById(requestedCompanyId));
    }
}

