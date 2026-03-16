export default function Layout({ children }) {
  return (
    <div className="flex min-h-screen bg-gray-100">
      {/* Sidebar */}
      <div className="w-64 bg-white border-r">
        <div className="p-6 text-xl font-bold">Forecast</div>

        <nav className="flex flex-col gap-2 p-4">
          <a className="p-2 rounded hover:bg-gray-100">Dashboard</a>

          <a className="p-2 rounded hover:bg-gray-100">Invoices</a>

          <a className="p-2 rounded hover:bg-gray-100">Recurring</a>

          <a className="p-2 rounded hover:bg-gray-100">Clients</a>
        </nav>
      </div>

      {/* Main area */}
      <div className="flex-1 flex flex-col">
        {/* Top bar */}
        <header className="h-16 bg-white border-b flex items-center justify-between px-6">
          <div className="font-medium">Dashboard</div>

          <div>User</div>
        </header>

        {/* Page content */}
        <main className="p-6">{children}</main>
      </div>
    </div>
  );
}
