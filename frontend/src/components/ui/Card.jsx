import clsx from "clsx";

export default function Card({ children, className }) {
  return (
    <div
      className={clsx(
        "rounded-xl border border-slate-200 bg-white p-4 shadow-sm",
        className
      )}
    >
      {children}
    </div>
  );
}
