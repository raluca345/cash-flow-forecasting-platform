import clsx from "clsx";

export default function Input({
  label,
  error,
  className,
  trailingElement,
  ...props
}) {
  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label className={clsx("text-sm text-slate-600", error && "text-red-600")}>
          {label}
        </label>
      )}

      <div className="relative">
        <input
          className={clsx(
            "w-full border border-slate-300 rounded-lg px-3 py-2",
            "focus:outline-none focus:ring-2 focus:ring-blue-500",
            trailingElement && "pr-11",
            className,
            error && "border-red-300 focus:border-red-500 focus:ring-red-500/20"
          )}
          {...props}
        />

        {trailingElement && (
          <div className="absolute inset-y-0 right-0 flex items-center pr-3">
            {trailingElement}
          </div>
        )}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
