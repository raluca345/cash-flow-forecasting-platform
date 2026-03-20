import { Navigate } from "react-router-dom";
import useAuthStore from "../store/authStore";

export default function RoleDashboardPage() {
  const role = useAuthStore((state) => state.role);

  if (role === "SYSTEM_ADMIN") {
    return <Navigate to="/admin" replace />;
  }

  if (role === "COMPANY_ADMIN" || role === "FINANCE") {
    return <Navigate to="/app" replace />;
  }

  return <Navigate to="/login" replace />;
}
