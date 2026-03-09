package org.forecast.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.forecast.backend.enums.Role;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_company_email", columnNames = {"company_id", "email"})
        },
        indexes = {
                @Index(name = "idx_user_company", columnList = "company_id"),
                @Index(name = "idx_user_company_email", columnList = "company_id,email")
        }
)
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "User name is required")
    @Size(min = 2, max = 255, message = "User name must be between 2 and 255 characters")
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 320, message = "Email must be at most 320 characters")
    @Column(nullable = false, length = 320)
    private String email;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Size(max = 2048, message = "Profile picture URL must be at most 2048 characters")
    @Column(length = 2048)
    private String profilePictureUrl;

    @NotNull(message = "Company is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
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
    }
}
