package org.forecast.backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    @Test
    void generateToken_includesRoleClaim_whenNotProvidedInExtraClaims() {
        JwtService svc = new JwtService();
        String base64Key = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(svc, "secretKey", base64Key);

        UserDetails user = Mockito.mock(UserDetails.class);
        when(user.getUsername()).thenReturn("jane@acme.test");
        Collection<GrantedAuthority> auths =
                List.of(new SimpleGrantedAuthority("ROLE_FINANCE"));
        when(user.getAuthorities()).thenAnswer(inv -> auths);

        String token = svc.generateToken(Map.of(), user);
        String role = svc.extractRole(token);

        assertThat(role).isEqualTo("FINANCE");
    }

    @Test
    void generateToken_preservesRoleInExtraClaims_ifProvided() {
        JwtService svc = new JwtService();
        String base64Key2 = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(svc, "secretKey", base64Key2);

        UserDetails user = Mockito.mock(UserDetails.class);
        when(user.getUsername()).thenReturn("john@acme.test");
        Collection<GrantedAuthority> auths2 =
                List.of(new SimpleGrantedAuthority("ROLE_FINANCE"));
        when(user.getAuthorities()).thenAnswer(inv -> auths2);

        String token = svc.generateToken(Map.of("role", "COMPANY_ADMIN"), user);
        String role = svc.extractRole(token);

        assertThat(role).isEqualTo("COMPANY_ADMIN");
    }
}

