package org.forecast.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "clients",
        indexes = {
                @Index(name = "idx_client_company", columnList = "company_id")
        }
)
@Getter
@Setter
public class Client {
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 255, message = "Client name must be between 2 and 255 characters")
    @Column(nullable = false)
    private String name;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    @Column(length = 100)
    private String email;

    /**
     * Pragmatic phone validation: allow +, digits, spaces, and common separators.
     */
    @Size(max = 32, message = "Phone number must be at most 32 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Phone number contains invalid characters")
    @Column(length = 32)
    private String phoneNumber;

    /**
     * Optional VAT number for clients that are registered businesses.
     */
    @Size(min = 8, max = 20, message = "VAT number must be between 8 and 20 characters")
    @Pattern(
            regexp = "[A-Z0-9]{8,20}",
            message = "VAT number must be uppercase alphanumeric with no spaces"
    )
    @Column(name = "vat_number", length = 20)
    private String vatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String address;
}
