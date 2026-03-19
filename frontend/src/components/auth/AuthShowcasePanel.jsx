export default function AuthShowcasePanel() {
  return (
    <aside className="relative hidden overflow-hidden bg-slate-950 text-white lg:flex lg:w-[52%]">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(99,102,241,0.2),transparent_32%),radial-gradient(circle_at_bottom_left,rgba(56,189,248,0.14),transparent_28%)]" />
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.04)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.04)_1px,transparent_1px)] bg-size-[32px_32px]" />

      <div className="relative z-10 flex w-full flex-col justify-between px-10 py-12 xl:px-14 xl:py-16">
        <div className="max-w-md">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-indigo-300/85">
            Cash Flow Forecasting Platform
          </p>

          <h2 className="mt-5 text-4xl font-semibold leading-tight text-white xl:text-5xl">
            See cash gaps before they happen
          </h2>

          <p className="mt-4 max-w-sm text-sm leading-6 text-slate-300 xl:text-base">
            Track invoices, recurring income, and runway visibility from one
            workspace designed for freelancers and lean finance teams.
          </p>
        </div>

        <div className="mt-12 max-w-xl space-y-4">
          <div className="rounded-[28px] border border-white/10 bg-white/5 p-5 shadow-2xl shadow-black/20 backdrop-blur">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-slate-400">
                  Net Cash Position
                </p>
                <p className="mt-2 text-3xl font-semibold">$48,200</p>
              </div>

              <span className="rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-medium text-emerald-300">
                +12.4%
              </span>
            </div>

            <div className="mt-6 rounded-2xl border border-white/6 bg-slate-900/40 p-4">
              <div className="flex items-end gap-3">
                <div className="flex h-28 w-full items-end gap-2">
                  <div
                    className="w-full rounded-t bg-slate-500/40"
                    style={{ height: "34%" }}
                  />
                  <div
                    className="w-full rounded-t bg-slate-500/40"
                    style={{ height: "44%" }}
                  />
                  <div
                    className="w-full rounded-t bg-slate-500/40"
                    style={{ height: "52%" }}
                  />
                  <div
                    className="w-full rounded-t bg-emerald-400/65"
                    style={{ height: "68%" }}
                  />
                  <div
                    className="w-full rounded-t bg-emerald-400/85"
                    style={{ height: "84%" }}
                  />
                </div>

                <div className="min-w-28 rounded-2xl border border-emerald-400/15 bg-emerald-400/10 p-3">
                  <p className="text-[11px] uppercase tracking-[0.18em] text-emerald-200/75">
                    Runway
                  </p>
                  <p className="mt-2 text-xl font-semibold text-emerald-200">
                    8.4 months
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
              <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">
                Overdue
              </p>
              <p className="mt-2 text-lg font-semibold">$6,200</p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
              <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">
                Recurring
              </p>
              <p className="mt-2 text-lg font-semibold">$12,900</p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
              <p className="text-[11px] uppercase tracking-[0.18em] text-slate-400">
                Paid On Time
              </p>
              <p className="mt-2 text-lg font-semibold">91%</p>
            </div>
          </div>
        </div>

        <div className="mt-10 flex flex-wrap gap-3 text-sm text-slate-300">
          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5">
            Invoice tracking
          </span>
          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5">
            Runway visibility
          </span>
          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5">
            Revenue forecasting
          </span>
        </div>
      </div>
    </aside>
  );
}
