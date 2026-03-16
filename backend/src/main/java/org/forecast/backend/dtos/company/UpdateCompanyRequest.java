package org.forecast.backend.dtos.company;

import jakarta.validation.constraints.Email;
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
public class UpdateCompanyRequest {

    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String name;

    @Size(max = 2048, message = "Logo URL must be at most 2048 characters")
    private String logoUrl;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 32, message = "Phone must be at most 32 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Phone contains invalid characters")
    private String phone;

    @Size(max = 2048, message = "Website URL must be at most 2048 characters")
    private String website;

    @Size(min = 15, max = 34, message = "IBAN must be between 15 and 34 characters")
    @Pattern(regexp = "[A-Z0-9]{15,34}", message = "IBAN must be uppercase alphanumeric with no spaces")
    private String iban;

    @Size(min = 8, max = 20, message = "VAT number must be between 8 and 20 characters")
    @Pattern(regexp = "[A-Z0-9]{8,20}", message = "VAT number must be uppercase alphanumeric with no spaces")
    private String vatNumber;
}

