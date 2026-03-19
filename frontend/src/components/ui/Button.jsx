import clsx from "clsx";

export default function Button({
  children,
  variant = "primary",
  size = "md",
  className = "",
  ...props
}) {
  const base = "rounded-lg font-medium transition";

  const variants = {
    primary: "bg-indigo-600 text-white hover:bg-indigo-700",
    secondary: "border border-indigo-600 text-indigo-600 hover:bg-indigo-50",
    danger: "bg-red-500 text-white hover:bg-red-600",
    ghost: "text-slate-600 hover:bg-slate-100",
  };

  const sizes = {
    sm: "px-3 py-1 text-sm",
    md: "px-4 py-2",
    lg: "px-6 py-3 text-lg",
  };

  return (
    <button
      className={clsx(base, variants[variant], sizes[size], className)}
      {...props}
    >
      {children}
    </button>
  );
}
