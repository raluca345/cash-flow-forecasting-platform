import Card from "./Card";

export default function BusinessHealthCard({ data }) {
  const [firstRow, secondRow] = data.rows;
  const status = data.status.toLowerCase();

  const statusClassName =
    {
      stable: "text-green-600",
      warning: "text-amber-600",
      danger: "text-red-600",
    }[status] ?? "text-slate-900";

  function getValueClassName(value) {
    if (typeof value === "string") {
      if (value.startsWith("+")) {
        return "text-green-600";
      }
      if (value.startsWith("-")) {
        return "text-red-600";
      }
    }

    return "text-slate-900";
  }

  return (
    <Card className="h-full">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        Business Health
      </p>
      <p className={`mt-2 text-2xl font-bold leading-none ${statusClassName}`}>
        {data.status}
      </p>
      <div className="mt-5 space-y-3">
        <div className="flex items-center justify-between gap-4">
          <span className="text-sm font-semibold text-slate-800">
            {firstRow.label}:
          </span>
          <span
            className={`text-sm font-semibold tabular-nums ${getValueClassName(firstRow.value)}`}
          >
            {firstRow.value}
          </span>
        </div>

        <div className="flex items-center justify-between gap-4">
          <span className="text-sm font-semibold text-slate-800">
            {secondRow.label}:
          </span>
          <span
            className={`text-sm font-semibold tabular-nums ${getValueClassName(secondRow.value)}`}
          >
            {secondRow.value}
          </span>
        </div>
      </div>
    </Card>
  );
}
