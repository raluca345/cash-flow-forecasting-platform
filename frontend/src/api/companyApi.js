import { apiFetch } from "./client";

export async function generateCompanyInviteCode(companyId) {
  if (!companyId) {
    throw new Error("Company context required");
  }

  return apiFetch(`/companies/${companyId}/invite`, {
    method: "POST",
  });
}
