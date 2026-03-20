import BusinessHealthCard from "../components/ui/BusinessHealthCard";
import Card from "../components/ui/Card";

export default function CompanyDashboardPage() {
  //TODO: when the analytics endpoint is implemented this function will be tied to
  //an useEffect hook
  function getBusinessHealthMetrics(data) {
    if (data.status === "stable") {
      return {
        status: "Stable",
        rows: [
          {
            label: "Net monthly trend",
            value: formatCurrency(data.netMonthlyTrend, "USD", {
              signed: true,
            }),
          },
          {
            label: "3-month average",
            value: formatCurrency(data.threeMonthAverage, "USD"),
          },
        ],
      };
    }

    return {
      status: capitalize(data.status),
      rows: [
        {
          label: "Net monthly trend",
          value: formatCurrency(data.netMonthlyTrend, "USD", { signed: true }),
        },
        {
          label: "Months till runway",
          value: formatMonths(data.monthsTillRunway),
        },
      ],
    };
  }

  function formatCurrency(value, currency, options = {}) {
    const { signed = false } = options;

    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      maximumFractionDigits: 0,
      signDisplay: signed ? "always" : "auto",
    }).format(value);
  }

  function formatMonths(value) {
    if (value == null || Number.isNaN(Number(value))) {
      return "N/A";
    }

    const months = Number(value);
    const displayValue = Number.isInteger(months) ? months : months.toFixed(1);

    return `${displayValue} ${months === 1 ? "month" : "months"}`;
  }

  function capitalize(value) {
    if (!value) {
      return "";
    }

    return value.charAt(0).toUpperCase() + value.slice(1);
  }

  const analytics = {
    status: "stable",
    netMonthlyTrend: 9200,
    threeMonthAverage: 9200,
    monthsTillRunway: 14,
  };

  const data = getBusinessHealthMetrics(analytics);

  return (
    <>
      <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <BusinessHealthCard data={data} />

        <Card>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Revenue
          </p>
          <p className="mt-2 text-2xl font-bold leading-none">$18,400</p>
        </Card>

        <Card>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Expenses
          </p>
          <p className="mt-2 text-2xl text-red-500 font-bold leading-none">
            $9,200
          </p>
        </Card>

        <Card>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Profit
          </p>
          <p className="mt-2 text-2xl font-bold leading-none text-green-600">
            $9,200
          </p>
        </Card>
      </div>

      <Card className="h-85">Chart goes here</Card>
    </>
  );
}
