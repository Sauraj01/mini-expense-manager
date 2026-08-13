import { useEffect, useMemo, useState } from "react";

type Expense = {
  id: number;
  date: string;
  amount: number;
  vendorName: string;
  description: string;
  category: string;
  anomaly: boolean;
};

type Dashboard = {
  month: string;
  monthlyTotalsByCategory: Record<string, number>;
  topVendors: { vendorName: string; total: number }[];
  anomalyCount: number;
  anomalies: Expense[];
};

type Rule = {
  id: number;
  vendorKeyword: string;
  category: string;
};

const money = (n: number) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR"
  }).format(n);

function App() {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [rules, setRules] = useState<Rule[]>([]);
  const [message, setMessage] = useState("");

  const [form, setForm] = useState({
    date: new Date().toISOString().slice(0, 10),
    amount: "",
    vendorName: "",
    description: ""
  });

  async function load() {
    const [expenseRes, dashboardRes, rulesRes] = await Promise.all([
      fetch("/api/expenses"),
      fetch(`/api/dashboard?month=${month}`),
      fetch("/api/rules")
    ]);

    setExpenses(await expenseRes.json());
    setDashboard(await dashboardRes.json());
    setRules(await rulesRes.json());
  }

  useEffect(() => {
    load().catch(() => setMessage("Unable to connect to backend."));
  }, [month]);

  async function addExpense(e: React.FormEvent) {
    e.preventDefault();

    const response = await fetch("/api/expenses", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ...form,
        amount: Number(form.amount)
      })
    });

    if (!response.ok) {
      setMessage("Could not save expense.");
      return;
    }

    setForm({
      date: new Date().toISOString().slice(0, 10),
      amount: "",
      vendorName: "",
      description: ""
    });
    setMessage("Expense added.");
    await load();
  }

  async function uploadCsv(file: File) {
    const data = new FormData();
    data.append("file", file);

    const response = await fetch("/api/expenses/upload", {
      method: "POST",
      body: data
    });

    const result = await response.json();

    if (!response.ok) {
      setMessage("CSV import failed.");
      return;
    }

    setMessage(`${result.imported} expenses imported.`);
    await load();
  }

  const total = useMemo(
    () => Object.values(dashboard?.monthlyTotalsByCategory ?? {}).reduce((a, b) => a + b, 0),
    [dashboard]
  );

  return (
    <div className="page">
      <header>
        <div>
          <h1>Mini Expense Manager</h1>
          <p>Track, categorize and detect unusual spending.</p>
        </div>
        <label className="month">
          Dashboard month
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
        </label>
      </header>

      {message && <div className="message">{message}</div>}

      <section className="cards">
        <div className="card">
          <span>Monthly spend</span>
          <strong>{money(total)}</strong>
        </div>
        <div className="card">
          <span>Categories</span>
          <strong>{Object.keys(dashboard?.monthlyTotalsByCategory ?? {}).length}</strong>
        </div>
        <div className="card danger">
          <span>Anomalies</span>
          <strong>{dashboard?.anomalyCount ?? 0}</strong>
        </div>
      </section>

      <main className="grid">
        <section className="panel">
          <h2>Add Expense</h2>
          <form onSubmit={addExpense}>
            <label>Date<input type="date" required value={form.date}
              onChange={(e) => setForm({...form, date: e.target.value})}/></label>
            <label>Amount<input type="number" min="0.01" step="0.01" required value={form.amount}
              onChange={(e) => setForm({...form, amount: e.target.value})}/></label>
            <label>Vendor Name<input required value={form.vendorName}
              placeholder="e.g. Swiggy"
              onChange={(e) => setForm({...form, vendorName: e.target.value})}/></label>
            <label>Description<textarea value={form.description}
              onChange={(e) => setForm({...form, description: e.target.value})}/></label>
            <button type="submit">Add Expense</button>
          </form>

          <h2 className="upload-title">CSV Upload</h2>
          <input type="file" accept=".csv"
            onChange={(e) => e.target.files?.[0] && uploadCsv(e.target.files[0])}/>
          <small>Columns: date, amount, vendorName, description</small>
        </section>

        <section className="panel">
          <h2>Monthly Category Totals</h2>
          {Object.entries(dashboard?.monthlyTotalsByCategory ?? {}).map(([category, amount]) => (
            <div className="bar-row" key={category}>
              <span>{category}</span>
              <div className="bar"><i style={{width: `${Math.min(100, (amount / Math.max(total, 1)) * 100)}%`}} /></div>
              <b>{money(amount)}</b>
            </div>
          ))}

          <h2>Top 5 Vendors</h2>
          <ol>
            {(dashboard?.topVendors ?? []).map((vendor) => (
              <li key={vendor.vendorName}>
                <span>{vendor.vendorName}</span>
                <b>{money(vendor.total)}</b>
              </li>
            ))}
          </ol>
        </section>
      </main>

      <section className="panel">
        <h2>Anomalies</h2>
        {dashboard?.anomalies.length === 0 && <p>No anomalies detected.</p>}
        <div className="anomaly-list">
          {dashboard?.anomalies.map((expense) => (
            <div className="anomaly" key={expense.id}>
              <div>
                <strong>{expense.vendorName}</strong>
                <span>{expense.category} · {expense.date}</span>
              </div>
              <b>{money(expense.amount)}</b>
            </div>
          ))}
        </div>
      </section>

      <section className="panel">
        <h2>Vendor Rules</h2>
        <div className="rules">
          {rules.map((rule) => (
            <span key={rule.id}>{rule.vendorKeyword} → {rule.category}</span>
          ))}
        </div>
      </section>

      <section className="panel">
        <h2>All Expenses</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Date</th><th>Vendor</th><th>Category</th><th>Amount</th><th>Status</th></tr>
            </thead>
            <tbody>
              {expenses.slice().reverse().map((expense) => (
                <tr key={expense.id} className={expense.anomaly ? "row-anomaly" : ""}>
                  <td>{expense.date}</td>
                  <td>{expense.vendorName}</td>
                  <td>{expense.category}</td>
                  <td>{money(expense.amount)}</td>
                  <td>{expense.anomaly ? "⚠ Anomaly" : "Normal"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default App;
