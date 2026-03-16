import Layout from "../components/layout/Layout";

export default function DashboardPage() {
  return (
    <Layout>
      {/* KPI cards */}
      <div className="grid grid-cols-4 gap-6 mb-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-gray-500 text-sm">Revenue</p>
          <p className="text-2xl font-bold">$18,400</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-gray-500 text-sm">Expenses</p>
          <p className="text-2xl font-bold">$9,200</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-gray-500 text-sm">Profit</p>
          <p className="text-2xl font-bold text-green-600">$9,200</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <p className="text-gray-500 text-sm">Overdue</p>
          <p className="text-2xl font-bold text-red-500">$1,200</p>
        </div>
      </div>

      {/* Chart area */}
      <div className="bg-white p-6 rounded-lg shadow h-96">Chart goes here</div>
    </Layout>
  );
}
