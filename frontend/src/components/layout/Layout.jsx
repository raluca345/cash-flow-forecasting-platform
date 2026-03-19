export default function Layout({ children }) {
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
          {/*TODO: add login/signup buttons here or the user's pfp + username*/}
          <div className="text-gray-800">User</div>
        </header>

        {/* Page content */}
        <main className="p-6 bg-gray-100 flex-1">
          <div className="max-w-7xl mx-auto">{children}</div>
        </main>
      </div>
    </div>
  );
}
