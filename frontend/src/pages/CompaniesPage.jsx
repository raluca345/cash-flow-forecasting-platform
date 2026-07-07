import { useCallback, useEffect, useState } from "react";
import { listCompanies, generateCompanyInviteCode } from "../api/companyApi";
import { formatExpiration } from "../lib/formatDate";
import Button from "../components/ui/Button";
import Table from "../components/ui/Table";

const COLUMNS = [
  { key: "name", label: "Name", className: "font-medium" },
  { key: "email", label: "Email" },
  {
    key: "inviteCode",
    label: "Invite Code",
    className: "font-mono tracking-wide",
    render: (row) => row.inviteCode || "—",
  },
  {
    key: "expiresAt",
    label: "Expires At",
    render: (row) => formatExpiration(row.inviteCodeExpiresAt) || "—",
  },
];

export default function CompaniesPage() {
  const [companies, setCompanies] = useState([]);
  const [filterActive, setFilterActive] = useState(false);
  const [generatingId, setGeneratingId] = useState(null);

  useEffect(() => {
    listCompanies()
      .then(setCompanies)
      .catch((error) => {
        console.log(error.message);
      });
  }, []);

  const handleGenerate = useCallback(async (companyId) => {
    setGeneratingId(companyId);
    try {
      const response = await generateCompanyInviteCode(companyId);
      setCompanies((prev) =>
        prev.map((c) =>
          c.id === companyId
            ? {
                ...c,
                inviteCode: response.inviteCode,
                inviteCodeExpiresAt: response.expiresAt,
              }
            : c,
        ),
      );
    } catch {
      // silent
    } finally {
      setGeneratingId(null);
    }
  }, []);

  const handleCopy = useCallback((code) => {
    navigator.clipboard.writeText(code);
  }, []);

  const displayed = filterActive
    ? companies.filter((c) => c.inviteCode != null)
    : companies;

  const columns = [
    ...COLUMNS,
    {
      key: "actions",
      label: "Actions",
      render: (company) =>
        company.inviteCode ? (
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => handleCopy(company.inviteCode)}
            >
              Copy
            </Button>
            <Button
              type="button"
              size="sm"
              disabled={generatingId === company.id}
              onClick={() => handleGenerate(company.id)}
            >
              Regenerate
            </Button>
          </div>
        ) : (
          <Button
            type="button"
            size="sm"
            disabled={generatingId === company.id}
            onClick={() => handleGenerate(company.id)}
          >
            Generate
          </Button>
        ),
    },
  ];

  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-950">
          Companies
        </h1>

        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
            checked={filterActive}
            onChange={(e) => setFilterActive(e.target.checked)}
          />
          Active codes only
        </label>
      </div>

      <ul className="mt-5 space-y-3">
        <li className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
          TODO: Build company creation flow (form validation, API integration,
          and success states).
        </li>
        <li className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
          TODO: Support company admin account creation from this area (create
          user + assign COMPANY_ADMIN role).
        </li>
        <li className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
          TODO: Pagination for the companies table.
        </li>
      </ul>

      <div className="mt-5">
        <Table
          columns={columns}
          data={displayed}
          keyExtractor={(row) => row.id}
        />
      </div>
    </div>
  );
}
