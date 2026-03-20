export default function SystemAdminDashboardPage() {
  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-slate-200 bg-white px-6 py-7 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-widest text-indigo-600">
          Platform Overview
        </p>
        <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">
          Manage companies and workspace access
        </h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
          Controls for onboarding companies and managing workspace access.
        </p>
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">
                Recent Company Activity
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Newly created workspaces and onboarding progress will appear
                here.
              </p>
            </div>
            <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-700">
              Coming soon
            </span>
          </div>

          <div className="mt-6 space-y-3">
            {["Acme Studio", "Northwind Labs", "Pioneer Advisory"].map(
              (company) => (
                <div
                  key={company}
                  className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
                >
                  <div>
                    <p className="font-medium text-slate-800">{company}</p>
                    <p className="text-sm text-slate-500">
                      Workspace setup timeline placeholder
                    </p>
                  </div>
                  <span className="text-sm font-medium text-slate-500">
                    Pending
                  </span>
                </div>
              ),
            )}
          </div>
        </section>

        <section className="rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-950">
            Workspace Access
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Invite flows, company admins, and provisioning checks will live in
            this area.
          </p>

          <div className="mt-6 space-y-4">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Company Admins
              </p>
              <p className="mt-2 text-2xl font-semibold text-slate-950">12</p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Active Companies
              </p>
              <p className="mt-2 text-2xl font-semibold text-slate-950">37</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
