export const systemAdminNavigation = [
  { label: "Dashboard", to: "/admin" },
  { label: "Companies", to: "/admin/companies" },
  { label: "Users", to: "/admin/users" },
];

export const companyNavigation = [
  { label: "Dashboard", to: "/app" },
  { label: "Invoices", to: "/app/invoices" },
  { label: "Recurring", to: "/app/recurring" },
  { label: "Clients", to: "/app/clients" },
];

export const companyAdminNavigation = [
  ...companyNavigation,
  { label: "Team", to: "/app/team" },
  { label: "Invite Codes", to: "/app/invite-codes" },
];
