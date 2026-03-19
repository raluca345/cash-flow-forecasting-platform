package org.forecast.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forecast.backend.enums.Role;
import org.forecast.backend.model.User;
import org.forecast.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SystemAdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.system-admin.email:}")
    private String systemAdminEmail;

    @Value("${app.bootstrap.system-admin.password:}")
    private String systemAdminPassword;

    @Value("${app.bootstrap.system-admin.name:System Administrator}")
    private String systemAdminName;

    @Bean
    public CommandLineRunner bootstrapSystemAdmin() {
        return args -> {
            if (systemAdminEmail == null || systemAdminEmail.isBlank()
                    || systemAdminPassword == null || systemAdminPassword.isBlank()) {
                return;
            }

            String normalizedEmail = systemAdminEmail.trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                return;
            }

            User user = new User();
            user.setName(systemAdminName);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(systemAdminPassword));
            user.setRole(Role.SYSTEM_ADMIN);
            user.setCompany(null);

            userRepository.save(user);
            log.info("Bootstrapped SYSTEM_ADMIN user {}", normalizedEmail);
        };
    }
}
