import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEye, faEyeSlash } from "@fortawesome/free-solid-svg-icons";
import AuthShowcasePanel from "../components/auth/AuthShowcasePanel";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";

export default function SignupPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-100 lg:flex">
      <section className="flex min-h-screen w-full items-center justify-center px-5 py-10 sm:px-8 lg:w-[48%] lg:px-12 xl:px-16">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-indigo-600">
              Forecast
            </p>
            <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">
              Create your workspace account
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Join your company workspace with an invite code and start tracking
              invoices, recurring revenue, and runway in one place.
            </p>
          </div>

          <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-28px_rgba(15,23,42,0.28)] sm:p-8">
            <div>
              <p className="hidden text-xs font-semibold uppercase tracking-[0.28em] text-indigo-600 lg:block">
                Forecast
              </p>
              <h1 className="mt-0 text-3xl font-semibold tracking-tight text-slate-950 lg:mt-4">
                Create your account
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Use your company invite code to join your team and access
                invoices, recurring revenue, and cash-flow visibility.
              </p>
            </div>

            <form className="mt-8 space-y-5">
              <Input
                label="Full name"
                type="text"
                placeholder="Jane Doe"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
              />

              <Input
                label="Work email"
                type="email"
                placeholder="you@company.com"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
              />

              <Input
                label="Invite code"
                type="text"
                placeholder="Enter your company invite code"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
              />

              <Input
                label="Password"
                type={showPassword ? "text" : "password"}
                placeholder="Create a password"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
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

              <Input
                label="Confirm password"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="Repeat your password"
                className="h-11 border-slate-200 bg-slate-50 focus:border-indigo-500 focus:ring-indigo-500/30"
                trailingElement={
                  <button
                    type="button"
                    onClick={() =>
                      setShowConfirmPassword((current) => !current)
                    }
                    className="text-slate-400 transition hover:text-indigo-600"
                    aria-label={
                      showConfirmPassword
                        ? "Hide password confirmation"
                        : "Show password confirmation"
                    }
                    aria-pressed={showConfirmPassword}
                  >
                    <FontAwesomeIcon
                      icon={showConfirmPassword ? faEyeSlash : faEye}
                      className="h-4 w-4"
                    />
                  </button>
                }
              />

              <div className="rounded-2xl border border-indigo-100 bg-indigo-50/70 px-4 py-3 text-sm text-slate-600">
                Your invite code links this account to your company workspace.
              </div>

              <Button type="submit" className="h-11 w-full rounded-xl">
                Create Account
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
              onClick={() => navigate("/login")}
              className="h-11 w-full rounded-xl"
            >
              Back to Sign In
            </Button>

            <p className="mt-6 text-center text-sm text-slate-500">
              Already have an account?{" "}
              <Link
                to="/login"
                className="font-medium text-indigo-600 transition hover:text-indigo-700"
              >
                Sign in here
              </Link>
            </p>
          </div>
        </div>
      </section>

      <AuthShowcasePanel />
    </div>
  );
}
