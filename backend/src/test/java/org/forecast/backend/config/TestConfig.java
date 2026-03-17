package org.forecast.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.forecast.backend.service.JwtService;
import org.jspecify.annotations.NonNull;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@TestConfiguration
@ConditionalOnProperty(
    name = "test.security.use-test-config",
    havingValue = "true",
    matchIfMissing = false)
public class TestConfig {
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  // Provide a simple filter that injects an authenticated principal during tests.
  @Bean
  public OncePerRequestFilter testAuthenticationFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain)
          throws ServletException, IOException {
        var auth =
            new UsernamePasswordAuthenticationToken(
                "test-user",
                null,
                List.of(
                    new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_COMPANY_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_FINANCE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
          filterChain.doFilter(request, response);
        } finally {
          SecurityContextHolder.clearContext();
        }
      }
    };
  }

  // Register the testAuthenticationFilter as a servlet filter so it precedes Spring Security
  // filters
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> testAuthenticationFilterRegistration() {
    // Avoid autowiring ambiguity by directly using the local bean factory method
    FilterRegistrationBean<OncePerRequestFilter> reg =
        new FilterRegistrationBean<>(testAuthenticationFilter());
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
    reg.setName("testAuthenticationFilter");
    return reg;
  }

  // Provide a mock JwtService so JwtAuthenticationFilter can be constructed in tests
  @Bean
  @ConditionalOnMissingBean(JwtService.class)
  public JwtService jwtService() {
    JwtService mock = Mockito.mock(JwtService.class);
    // default permissive behavior for controller tests
    Mockito.when(mock.extractUsername(Mockito.anyString())).thenReturn("test-user");
    Mockito.when(mock.extractRole(Mockito.anyString())).thenReturn("SYSTEM_ADMIN");
    Mockito.when(mock.isTokenValid(Mockito.anyString(), Mockito.any())).thenReturn(true);
    return mock;
  }

  // Minimal UserDetailsService used by security components in tests. Renamed to avoid bean name
  // collision with application config.
  @Bean(name = "testUserDetailsService")
  @ConditionalOnMissingBean(UserDetailsService.class)
  public UserDetailsService testUserDetailsService() {
    return username ->
        new User("test-user", "", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
  }

  // Provide the JwtAuthenticationFilter bean wired with the test JwtService and UserDetailsService
  @Bean
  @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtService jwtService, UserDetailsService userDetailsService) {
    return new JwtAuthenticationFilter(jwtService, userDetailsService);
  }

  // Provide a permissive security filter chain for controller tests so they don't require auth.
  // Only register this bean when there is no SecurityFilterChain already (e.g. in @WebMvcTest
  // slices).
  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .addFilterBefore(testAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
