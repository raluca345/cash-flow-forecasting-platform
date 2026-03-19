import clsx from "clsx";

export default function Input({ label, className, ...props }) {
  return (
    <div className="flex flex-col gap-1">
      {label && <label className="text-sm text-slate-600">{label}</label>}

      <input
        className={clsx(
          "border border-slate-300 rounded-lg px-3 py-2",
          "focus:outline-none focus:ring-2 focus:ring-blue-500",
          className,
        )}
        {...props}
      />
    </div>
  );
}
