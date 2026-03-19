import { useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEye, faEyeSlash } from "@fortawesome/free-solid-svg-icons";
import { useNavigate } from "react-router-dom";
import AuthShowcasePanel from "../components/auth/AuthShowcasePanel";
import { login } from "../api/authApi";
import { validateLoginForm } from "../lib/validation";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";
import useAuthStore from "../store/authStore";

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [form, setForm] = useState({
    email: "",
    password: "",
    rememberMe: true,
  });

  const navigate = useNavigate();
  const setAuthSession = useAuthStore((state) => state.setAuthSession);

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }

    const nextErrors = validateLoginForm(form);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setFormError("Please fix the errors below");
      return;
    }

    setFormError("");
    setIsSubmitting(true);

    try {
      const response = await login({
        email: form.email.trim(),
        password: form.password,
      });
      setAuthSession(response, form.rememberMe);
      navigate("/");
    } catch (error) {
      setFieldErrors(error.fieldErrors ?? {});
      setFormError(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateField(field, value) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    setFieldErrors((current) => {
      if (!current[field]) {
        return current;
      }

      const nextErrors = { ...current };
      delete nextErrors[field];
      return nextErrors;
    });
    setFormError("");
  }

  return (
    <div className="min-h-screen bg-slate-100 lg:flex">
      <section className="flex min-h-screen w-full items-center justify-center px-5 py-10 sm:px-8 lg:w-[48%] lg:px-12 xl:px-16">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-indigo-600">
              Forecast
            </p>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">
              Sign in to your workspace
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Track invoices, monitor recurring income, and keep cash flow in
              view from one place.
            </p>
          </div>

          <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-28px_rgba(15,23,42,0.28)] sm:p-8">
            <div>
              <p className="hidden text-xs font-semibold uppercase tracking-[0.28em] text-indigo-600 lg:block">
                Forecast
              </p>
              <h1 className="mt-0 text-3xl font-semibold tracking-tight text-slate-950 lg:mt-4">
                Sign in to your workspace
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Use your company account to manage invoices, recurring revenue,
                and cash runway.
              </p>
            </div>

            <form className="mt-8 space-y-5" onSubmit={handleSubmit} noValidate>
              {formError && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {formError}
                </div>
              )}

              <Input
                label="Work email"
                type="email"
                placeholder="you@company.com"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
                value={form.email}
                onChange={(event) => updateField("email", event.target.value)}
                error={fieldErrors.email}
              />

              <Input
                label="Password"
                type={showPassword ? "text" : "password"}
                placeholder="Enter your password"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
                value={form.password}
                onChange={(event) =>
                  updateField("password", event.target.value)
                }
                error={fieldErrors.password}
                trailingElement={
                  <button
                    type="button"
                    onClick={() => setShowPassword((current) => !current)}
                    className="text-slate-400 transition hover:text-indigo-600"
                    aria-label={
                      showPassword ? "Hide password" : "Show password"
                    }
                    aria-pressed={showPassword}
                  >
                    <FontAwesomeIcon
                      icon={showPassword ? faEyeSlash : faEye}
                      className="h-4 w-4"
                    />
                  </button>
                }
              />

              <div className="flex items-center justify-between gap-3 text-sm">
                <label className="flex items-center gap-2 text-slate-600">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                    checked={form.rememberMe}
                    onChange={(event) =>
                      updateField("rememberMe", event.target.checked)
                    }
                  />
                  Remember me
                </label>

                <button
                  type="button"
                  className="font-medium text-indigo-600 transition hover:text-indigo-700"
                >
                  Forgot password?
                </button>
              </div>

              <Button
                type="submit"
                className="h-11 w-full rounded-xl"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Signing in..." : "Sign In"}
              </Button>
            </form>

            <div className="my-6 flex items-center gap-4">
              <div className="h-px flex-1 bg-slate-200" />
              <span className="text-xs font-medium uppercase tracking-[0.22em] text-slate-400">
                Or
              </span>
              <div className="h-px flex-1 bg-slate-200" />
            </div>

            <Button
              variant="secondary"
              type="button"
              onClick={() => navigate("/signup")}
              className="h-11 w-full rounded-xl"
            >
              Sign Up
            </Button>

            <p className="mt-6 text-center text-sm text-slate-500">
              Need an invite code? Contact your company admin or platform
              administrator.
            </p>
          </div>
        </div>
      </section>

      <AuthShowcasePanel />
    </div>
  );
}
