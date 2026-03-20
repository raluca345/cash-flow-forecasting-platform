import { Navigate, Outlet } from "react-router-dom";
import Layout from "../components/layout/Layout";
import { systemAdminNavigation } from "../components/layout/navigation";
import useAuthStore from "../store/authStore";

export default function SystemAdminLayout() {
  const role = useAuthStore((state) => state.role);

  if (role !== "SYSTEM_ADMIN") {
    return <Navigate to="/" replace />;
  }

  return (
    <Layout navigationItems={systemAdminNavigation} defaultTitle="Dashboard">
      <Outlet />
    </Layout>
  );
}
