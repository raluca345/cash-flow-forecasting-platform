import { Navigate } from "react-router-dom";
import useAuthStore from "../store/authStore";

export default function TeamPage() {
  const role = useAuthStore((state) => state.role);

  if (role !== "COMPANY_ADMIN") {
    return <Navigate to="/app" replace />;
  }

  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-950">
        Team
      </h1>
      <p className="mt-3 text-sm leading-6 text-slate-600">
        Manage the users who belong to your company workspace here.
      </p>
    </div>
  );
}
