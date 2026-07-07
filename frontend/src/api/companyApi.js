import { apiFetch } from "./httpClient";

export async function listCompanies() {
  return apiFetch("/companies");
}

export async function generateCompanyInviteCode(companyId) {
  if (!companyId) {
    throw new Error("Company context required");
  }

  return apiFetch(`/companies/${companyId}/invite`, {
    method: "POST",
  });
}
