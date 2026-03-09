package org.forecast.backend.dtos.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.forecast.backend.model.Company;

import java.util.List;
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
    private String phoneNumber;
    private String website;
    private String iban;
    private String vatNumber;

    public static CompanyResponse fromEntity(Company company) {
        if (company == null) return null;
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .logoUrl(company.getLogoUrl())
                .address(company.getAddress())
                .email(company.getEmail())
                .phoneNumber(company.getPhoneNumber())
                .website(company.getWebsite())
                .iban(company.getIban())
                .vatNumber(company.getVatNumber())
                .build();
    }

    public static List<CompanyResponse> fromEntities(List<Company> companies) {
        return companies == null ? List.of() : companies.stream().map(CompanyResponse::fromEntity).toList();
    }
}

