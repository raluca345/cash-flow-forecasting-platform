import { BrowserRouter, Routes, Route } from "react-router-dom";
import ProtectedRoute from "../components/auth/ProtectedRoute";
import PublicOnlyRoute from "../components/auth/PublicOnlyRoute";
import LoginPage from "../pages/LoginPage";
import SignupPage from "../pages/SignupPage";
import RoleDashboardPage from "../pages/RoleDashboardPage";
import CompaniesPage from "../pages/CompaniesPage";
import UsersPage from "../pages/UsersPage";
import InvoicesPage from "../pages/InvoicesPage";
import RecurringPage from "../pages/RecurringPage";
import ClientsPage from "../pages/ClientsPage";
import TeamPage from "../pages/TeamPage";
import InviteCodesPage from "../pages/InviteCodesPage";
import CompanyDashboardPage from "../pages/CompanyDashboardPage";
import SystemAdminDashboardPage from "../pages/SystemAdminDashboardPage";
import CompanyLayout from "../pages/CompanyLayout";
import SystemAdminLayout from "../pages/SystemAdminLayout";

export default function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<RoleDashboardPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/admin" element={<SystemAdminLayout />}>
            <Route index element={<SystemAdminDashboardPage />} />
            <Route path="companies" element={<CompaniesPage />} />
            <Route path="users" element={<UsersPage />} />
          </Route>

          <Route path="/app" element={<CompanyLayout />}>
            <Route index element={<CompanyDashboardPage />} />
            <Route path="invoices" element={<InvoicesPage />} />
            <Route path="recurring" element={<RecurringPage />} />
            <Route path="clients" element={<ClientsPage />} />
            <Route path="team" element={<TeamPage />} />
            <Route path="invite-codes" element={<InviteCodesPage />} />
          </Route>
        </Route>

        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
