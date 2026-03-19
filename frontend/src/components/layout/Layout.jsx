import { useNavigate } from "react-router-dom";
import Button from "../ui/Button";
import useAuthStore from "../../store/authStore";

export default function Layout({ children }) {
  const name = useAuthStore((state) => state.name);
  const role = useAuthStore((state) => state.role);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();

  function handleLogout() {
    clearAuth();
    navigate("/login", { replace: true });
  }

  return (
    <div className="flex min-h-screen bg-gray-100">
      {/* Sidebar */}
      <div className="w-52 bg-linear-to-b from-indigo-600 to-indigo-700">
        <div className="p-5 text-white text-xl font-semibold tracking-wide">
          Forecast
        </div>

        <nav className="flex flex-col gap-1.5 p-3">
          <a className="p-2 rounded text-white/90 hover:text-white hover:bg-black/10 transition">
            Dashboard
          </a>

          <a className="p-2 rounded text-white/90 hover:text-white hover:bg-black/10 transition">
            Invoices
          </a>

          <a className="p-2 rounded text-white/90 hover:text-white hover:bg-black/10 transition">
            Recurring
          </a>

          <a className="p-2 rounded text-white/90 hover:text-white hover:bg-black/10 transition">
            Clients
          </a>
        </nav>
      </div>

      {/* Main area */}
      <div className="flex-1 flex flex-col">
        {/* Top bar */}
        <header className="h-16 bg-white border-b flex items-center justify-between px-6">
          <div className="font-medium text-gray-800">Dashboard</div>
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              Log Out
            </Button>

            <div className="flex items-center gap-3 rounded-full border border-slate-200 bg-slate-50 px-2 py-1.5">
              <div className="h-9 w-9 rounded-full border border-indigo-200 bg-indigo-100" />

              <div className="text-right pr-2">
                <div className="font-medium text-gray-800">{name ?? "User"}</div>
                {role && (
                  <div className="text-xs uppercase tracking-wide text-gray-500">
                    {role.replaceAll("_", " ")}
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="p-6 bg-gray-100 flex-1">
          <div className="max-w-7xl mx-auto">{children}</div>
        </main>
      </div>
    </div>
  );
}
