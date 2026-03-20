import { Navigate, Outlet } from "react-router-dom";
import Layout from "../components/layout/Layout";
import {
  companyAdminNavigation,
  companyNavigation,
} from "../components/layout/navigation";
import useAuthStore from "../store/authStore";

export default function CompanyLayout() {
  const role = useAuthStore((state) => state.role);

  if (role !== "COMPANY_ADMIN" && role !== "FINANCE") {
    return <Navigate to="/" replace />;
  }

  const navigationItems =
    role === "COMPANY_ADMIN" ? companyAdminNavigation : companyNavigation;

  return (
    <Layout navigationItems={navigationItems} defaultTitle="Dashboard">
      <Outlet />
    </Layout>
  );
}
