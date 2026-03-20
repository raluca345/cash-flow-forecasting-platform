import { useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import Button from "../components/ui/Button";
import { generateCompanyInviteCode } from "../api/companyApi";
import useAuthStore from "../store/authStore";

export default function InviteCodesPage() {
  const role = useAuthStore((state) => state.role);
  const companyId = useAuthStore((state) => state.companyId);

  const [inviteCode, setInviteCode] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [formError, setFormError] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  const formattedExpiration = useMemo(() => {
    if (!expiresAt) {
      return "";
    }

    const date = new Date(expiresAt);
    if (Number.isNaN(date.getTime())) {
      return "";
    }

    return new Intl.DateTimeFormat("en-US", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(date);
  }, [expiresAt]);

  if (role !== "COMPANY_ADMIN") {
    return <Navigate to="/app" replace />;
  }

  async function handleGenerateInviteCode() {
    if (isGenerating) {
      return;
    }

    setFormError("");
    setIsGenerating(true);

    try {
      const response = await generateCompanyInviteCode(companyId);
      setInviteCode(response?.inviteCode ?? "");
      setExpiresAt(response?.expiresAt ?? "");
    } catch (error) {
      setFormError(error?.message ?? "Could not generate invite code");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-950">
        Invite Codes
      </h1>
      <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
        Generate and share invite codes so users can join your company
        workspace.
      </p>

      {formError && (
        <div className="mt-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {formError}
        </div>
      )}

      <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          Active invite code
        </p>

        <p className="mt-2 text-2xl font-semibold tracking-[0.08em] text-slate-950">
          {inviteCode || "Not generated yet"}
        </p>

        <p className="mt-2 text-sm text-slate-600">
          {formattedExpiration
            ? `Expires on ${formattedExpiration}`
            : "Generate a code to see expiration details"}
        </p>

        <Button
          type="button"
          onClick={handleGenerateInviteCode}
          disabled={isGenerating}
          className="mt-5"
        >
          {isGenerating ? "Generating..." : "Generate New Invite Code"}
        </Button>
      </div>
    </div>
  );
}
