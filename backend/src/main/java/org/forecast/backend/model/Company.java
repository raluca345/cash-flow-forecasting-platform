package org.forecast.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_iban", columnNames = {"iban"}),
                @UniqueConstraint(name = "uk_company_vat", columnNames = {"vat_number"})
        },
        indexes = {
                @Index(name = "idx_company_name", columnList = "name")
        }
)
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    @Column(nullable = false, length = 255)
    private String name;

    @Size(max = 2048, message = "Logo URL must be at most 2048 characters")
    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Size(max = 500, message = "Address must be at most 500 characters")
    @NotBlank(message = "Address is required")
    @Column(nullable = false, length = 500)
    private String address;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be at most 100 characters")
    @Column(nullable = false, length = 100)
    private String email;

    /**
     * Pragmatic phone validation: allow +, digits, spaces, and common separators.
     */
    @NotBlank(message = "Phone number is required")
    @Size(max = 32, message = "Phone number must be at most 32 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Phone number contains invalid characters")
    @Column(nullable = false, length = 32)
    private String phoneNumber;

    @Size(max = 2048, message = "Website URL must be at most 2048 characters")
    @NotBlank(message = "Website is required")
    @Column(nullable = false, length = 2048)
    private String website;

    /**
     * IBAN format varies by country; we validate a common, simple form: 15-34 alphanumeric, no spaces.
     * You can store with spaces in UI but normalize before saving.
     */
    @Size(min = 15, max = 34, message = "IBAN must be between 15 and 34 characters")
    @NotBlank(message = "IBAN is required")
    @Pattern(
            regexp = "[A-Z0-9]{15,34}",
            message = "IBAN must be uppercase alphanumeric with no spaces"
    )
    @Column(nullable = false, length = 34)
    private String iban;

    /**
     * VAT number formats vary; this is a pragmatic constraint: 8-20 uppercase alphanumeric.
     */
    @Size(min = 8, max = 20, message = "VAT number must be between 8 and 20 characters")
    @NotBlank(message = "VAT number is required")
    @Pattern(
            regexp = "[A-Z0-9]{8,20}",
            message = "VAT number must be uppercase alphanumeric with no spaces"
    )
    @Column(name = "vat_number", nullable = false, length = 20)
    private String vatNumber;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonManagedReference
    private List<User> users = new ArrayList<>();

    @Size(max = 64, message = "Invite code must be at most 64 characters")
    @Column(name = "invite_code", length = 64, unique = true)
    private String inviteCode;

    @Column(name = "invite_code_expires_at")
    private Instant inviteCodeExpiresAt;
}
