package org.forecast.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.model.Company;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {

    private UUID id;
    private String name;
    private String logoUrl;
    private String address;

    private String email;
    private String phone;
    private String website;

    private String iban;
    private String vatNumber;

    public static CompanyResponse fromEntity(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .logoUrl(company.getLogoUrl())
                .address(company.getAddress())
                .email(company.getEmail())
                .phone(company.getPhone())
                .website(company.getWebsite())
                .iban(company.getIban())
                .vatNumber(company.getVatNumber())
                .build();
    }
}

