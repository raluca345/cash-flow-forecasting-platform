package org.forecast.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.forecast.backend.enums.Role;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = {"email"})
        },
        indexes = {
                @Index(name = "idx_user_company", columnList = "company_id")
        }
)
@Getter
@Setter
public class User implements UserDetails {
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "User name is required")
    @Size(min = 2, max = 255, message = "User name must be between 2 and 255 characters")
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    @Column(nullable = false, length = 100)
    private String email;

    // May be null for OAuth-only users
    @Size(max = 255, message = "Password hash must be at most 255 characters")
    private String passwordHash;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.FINANCE;

    @Size(max = 2048, message = "Profile picture URL must be at most 2048 characters")
    @Column(length = 2048)
    private String profilePictureUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonBackReference
    private Company company;

    @PrePersist
    @PreUpdate
    protected void normalize() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (name != null) {
            name = name.trim();
        }
        // Ensure a persisted user always has a role set. Defensive: if role was
        // accidentally cleared, restore the safe default before persisting.
        if (this.role == null) {
            this.role = Role.FINANCE;
        }
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public @NonNull String getUsername() {
        return this.email;
    }
}
