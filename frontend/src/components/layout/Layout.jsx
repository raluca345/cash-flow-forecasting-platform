import { useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAnglesLeft,
  faAnglesRight,
  faChevronDown,
} from "@fortawesome/free-solid-svg-icons";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import Button from "../ui/Button";
import Dropdown from "../ui/Dropdown";
import useAuthStore from "../../store/authStore";

export default function Layout({
  children,
  navigationItems = [],
  defaultTitle = "Dashboard",
}) {
  const name = useAuthStore((state) => state.name);
  const role = useAuthStore((state) => state.role);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const location = useLocation();
  const navigate = useNavigate();
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  const displayName = name ?? "User";
  const avatarLetter = displayName.trim().charAt(0).toUpperCase() || "?";
  const currentNavigationItem = navigationItems.find(
    (item) =>
      location.pathname === item.to ||
      location.pathname.startsWith(`${item.to}/`),
  );
  const headerTitle = currentNavigationItem?.label ?? defaultTitle;

  function handleLogout() {
    clearAuth();
    navigate("/login", { replace: true });
  }

  function formatRole(roleName) {
    if (!roleName) {
      return "";
    }

    return roleName
      .toLowerCase()
      .split("_")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
  }

  return (
    <div className="flex min-h-screen bg-slate-100">
      <div
        className={[
          "shrink-0 overflow-hidden bg-linear-to-b from-indigo-600 to-indigo-700 transition-[width] duration-200 ease-out",
          isSidebarOpen ? "w-56" : "w-16",
        ].join(" ")}
      >
        <div
          className={[
            "flex h-16 items-center",
            isSidebarOpen ? "justify-between px-4" : "justify-center",
          ].join(" ")}
        >
          {isSidebarOpen && (
            <div className="text-white text-xl font-semibold tracking-wide">
              Forecast
            </div>
          )}

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setIsSidebarOpen((current) => !current)}
            className="h-9 w-9 rounded-lg px-0 py-0 text-white/90 hover:bg-black/15 hover:text-white"
            aria-label={isSidebarOpen ? "Collapse sidebar" : "Expand sidebar"}
            title={isSidebarOpen ? "Collapse sidebar" : "Expand sidebar"}
          >
            <FontAwesomeIcon
              icon={isSidebarOpen ? faAnglesLeft : faAnglesRight}
              className="h-4 w-4"
            />
          </Button>
        </div>

        {isSidebarOpen && (
          <nav className="flex flex-col gap-1.5 p-3">
            {navigationItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/admin" || item.to === "/app"}
                className={({ isActive }) =>
                  [
                    "rounded px-3 py-2 text-sm transition",
                    isActive
                      ? "bg-black/15 text-white"
                      : "text-white/90 hover:bg-black/10 hover:text-white",
                  ].join(" ")
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        )}
      </div>

      <div className="flex-1 flex flex-col">
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6">
          <div className="font-medium text-slate-800">{headerTitle}</div>
          <div className="flex items-center">
            <Dropdown
              align="right"
              trigger={({ isOpen, toggle }) => (
                <button
                  type="button"
                  onClick={toggle}
                  className={[
                    "flex items-center gap-3 rounded-full border px-2 py-1.5 transition",
                    isOpen
                      ? "border-indigo-200 bg-indigo-50"
                      : "border-slate-200 bg-slate-50 hover:border-slate-300",
                  ].join(" ")}
                  aria-haspopup="menu"
                  aria-expanded={isOpen}
                  title="Open account menu"
                >
                  <div className="flex h-9 w-9 items-center justify-center rounded-full border border-indigo-200 bg-indigo-100 text-sm font-semibold text-indigo-700">
                    {avatarLetter}
                  </div>

                  <div className="text-right">
                    <div className="font-medium text-slate-800">
                      {displayName}
                    </div>
                    {role && (
                      <div className="text-xs uppercase tracking-wide text-slate-500">
                        {formatRole(role)}
                      </div>
                    )}
                  </div>

                  <FontAwesomeIcon
                    icon={faChevronDown}
                    className={[
                      "h-3 w-3 pr-1 text-slate-400 transition-transform",
                      isOpen ? "rotate-180" : "",
                    ].join(" ")}
                  />
                </button>
              )}
            >
              {({ close }) => (
                <Button
                  type="button"
                  variant="dangerGhost"
                  size="sm"
                  onClick={() => {
                    close();
                    handleLogout();
                  }}
                  className="mx-2 my-1 block text-left"
                >
                  Log Out
                </Button>
              )}
            </Dropdown>
          </div>
        </header>

        <main className="flex-1 bg-slate-100 p-6">
          <div className="mx-auto max-w-7xl">{children}</div>
        </main>
      </div>
    </div>
  );
}
