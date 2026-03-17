package org.forecast.backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.dtos.company.CreateCompanyRequest;
import org.forecast.backend.dtos.company.UpdateCompanyRequest;
import org.forecast.backend.exceptions.ResourceNotFoundException;
import org.forecast.backend.model.Company;
import org.forecast.backend.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

  private final CompanyRepository companyRepository;
  private final CompanySecurityService companySecurityService;

  @Value("${app.invite.ttl.hours:168}")
  private long inviteTtlHours = 168;

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static String randomAlphaNumeric() {
    String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // avoid ambiguous chars
    SecureRandom rnd = new SecureRandom();
    StringBuilder sb = new StringBuilder(8);
    for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
    return sb.toString();
  }

  public Company create(CreateCompanyRequest request) {
    companySecurityService.requireSystemAdmin("Only system admins can create companies.");
    Company company = new Company();
    company.setName(requireNonBlank(request.getName(), "Company name is required"));
    company.setLogoUrl(null);
    company.setAddress(requireNonBlank(request.getAddress(), "Address is required"));
    company.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
    company.setPhoneNumber(requireNonBlank(request.getPhoneNumber(), "Phone is required"));
    company.setWebsite(requireNonBlank(request.getWebsite(), "Website is required"));
    company.setIban(requireNonBlank(request.getIban(), "IBAN is required"));
    company.setVatNumber(requireNonBlank(request.getVatNumber(), "VAT number is required"));
    return companyRepository.save(company);
  }

  public List<Company> listAll() {
    if (companySecurityService.isSystemAdmin()) {
      return companyRepository.findAll();
    }
    UUID currentCompanyId = companySecurityService.requireCurrentCompanyId("Access denied.");
    return List.of(getById(currentCompanyId));
  }

  public Company getById(UUID id) {
    Company company =
        companyRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("No company with id " + id + " found."));

    if (!companySecurityService.isSystemAdmin()) {
      companySecurityService.assertCompanyAccess(company.getId(), "Cannot access another company");
    }

    return company;
  }

  public Company update(UUID id, UpdateCompanyRequest request) {
    requireCompanyAdminCompanyAccess(id);
    Company company = getById(id);

    if (request.getName() != null)
      company.setName(requireNonBlank(request.getName(), "Company name is required"));
    if (request.getLogoUrl() != null)
      company.setLogoUrl(requireNonBlank(request.getLogoUrl(), "Logo URL is required"));
    if (request.getAddress() != null)
      company.setAddress(requireNonBlank(request.getAddress(), "Address is required"));
    if (request.getEmail() != null)
      company.setEmail(requireNonBlank(request.getEmail(), "Email is required"));
    if (request.getPhone() != null)
      company.setPhoneNumber(requireNonBlank(request.getPhone(), "Phone is required"));
    if (request.getWebsite() != null)
      company.setWebsite(requireNonBlank(request.getWebsite(), "Website is required"));
    if (request.getIban() != null)
      company.setIban(requireNonBlank(request.getIban(), "IBAN is required"));
    if (request.getVatNumber() != null)
      company.setVatNumber(requireNonBlank(request.getVatNumber(), "VAT number is required"));

    return companyRepository.save(company);
  }

  public Company updateLogoUrl(UUID id, String logoUrl) {
    requireCompanyAdminCompanyAccess(id);
    Company company = getById(id);
    company.setLogoUrl(logoUrl);
    return companyRepository.save(company);
  }

  /**
   * Generate and persist an invite code for the given company. Only company admins should call
   * this. This method will retry a few times if the randomly generated code collides with an
   * existing one.
   */
  public String generateInviteCode(UUID companyId) {
    requireCompanyAdminCompanyAccess(companyId);
    Company company = getById(companyId);

    for (int attempt = 0; attempt < 5; attempt++) {
      String code = randomAlphaNumeric();
      company.setInviteCode(code);
      company.setInviteCodeExpiresAt(Instant.now().plusSeconds(inviteTtlHours * 3600));
      try {
        companyRepository.save(company);
        return code;
      } catch (DataIntegrityViolationException e) {
        // likely invite_code unique constraint collision; try again
      }
    }

    throw new IllegalStateException(
        "Unable to generate unique invite code after multiple attempts");
  }

  private void requireCompanyAdminCompanyAccess(UUID companyId) {
    companySecurityService.requireCompanyAdmin(
        "Only company admins can manage company profiles or invite codes.");
    companySecurityService.assertCompanyAccess(companyId, "Access denied.");
  }
}
