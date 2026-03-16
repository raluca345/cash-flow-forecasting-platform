package org.forecast.backend.dtos.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 255, message = "Client name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 32, message = "Phone must be at most 32 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Phone contains invalid characters")
    private String phoneNumber;

    @Size(min = 8, max = 20, message = "VAT number must be between 8 and 20 characters")
    @Pattern(
            regexp = "[A-Z0-9]{8,20}",
            message = "VAT number must be uppercase alphanumeric with no spaces"
    )
    private String vatNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
}

