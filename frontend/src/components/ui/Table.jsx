import clsx from "clsx";

export default function Table({
  columns,
  data,
  keyExtractor,
  page,
  totalPages,
  onPageChange,
}) {
  return (
    <table className="w-full">
      <thead>
        <tr className="border-b border-slate-200 text-left text-sm font-semibold text-slate-500">
          {columns.map((col) => (
            <th
              key={col.key}
              className={clsx("px-4 py-3", col.headerClassName)}
            >
              {col.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row) => (
          <tr
            key={keyExtractor(row)}
            className="border-b border-slate-100 text-sm text-slate-700"
          >
            {columns.map((col) => (
              <td key={col.key} className={clsx("px-4 py-3", col.className)}>
                {col.render ? col.render(row) : row[col.key] ?? "—"}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
