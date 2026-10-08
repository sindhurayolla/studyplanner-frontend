import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import Plan from "./Plan";
import Subjects from "./Subjects";
import Tasks from "./Tasks";
import Profile from "./Profile";
import Progress from "./Progress";

const TABS = ["Overview", "Plan", "Subjects", "Tasks", "Progress", "Profile"];

export default function Dashboard({ user, onUserUpdate, onLogout }) {
  const [tab, setTab] = useState("Overview");
  const [data, setData] = useState({ subjects: [], tasks: [], plan: [], insights: null, reminders: [] });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const o = { userId: user.id };
      const [subjects, tasks, plan, insights, reminders] = await Promise.all([
        api("/subjects", o),
        api("/tasks", o),
        api("/plan", o),
        api("/insights", o),
        api("/reminders", o),
      ]);
      setData({ subjects, tasks, plan, insights, reminders });
    } catch (err) {
      setError(err.message);
    }
  }, [user.id]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Calls the API, then reloads everything (plan, insights and reminders depend on each other).
  const act = useCallback(
    async (path, method = "POST", body) => {
      setError("");
      setBusy(true);
      try {
        const result = await api(path, { method, body, userId: user.id });
        await refresh();
        return result;
      } catch (err) {
        setError(err.message);
      } finally {
        setBusy(false);
      }
    },
    [refresh, user.id]
  );

  const props = { user, data, act, busy };

  return (
    <div className="dash">
      <header>
        <h1>📚 AI Study Planner</h1>
        <span>Hi, {user.name}</span>
        <button onClick={onLogout}>Log out</button>
      </header>
      <nav className="tabs">
        {TABS.map((t) => (
          <button key={t} className={tab === t ? "" : "link"} onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </nav>
      {error && <p className="err">{error}</p>}
      {tab === "Overview" && <Overview {...props} goTo={setTab} />}
      {tab === "Plan" && <Plan {...props} />}
      {tab === "Subjects" && <Subjects {...props} />}
      {tab === "Tasks" && <Tasks {...props} />}
      {tab === "Progress" && <Progress {...props} />}
      {tab === "Profile" && <Profile {...props} onUserUpdate={onUserUpdate} />}
    </div>
  );
}

function Overview({ user, data, goTo, act, busy }) {
  const today = new Date().toLocaleDateString("en-CA");
  const todays = data.plan.filter((e) => e.date === today);
  const ins = data.insights;
  return (
    <>
      {user.goal && <p className="goal">🎯 Goal: {user.goal}</p>}
      {data.subjects.length === 0 && (
        <section className="card hero">
          <h2>Welcome! Let's build your personalised plan.</h2>
          <p className="muted">
            Add your subjects, exam dates and test scores, or load sample data to see the AI planner in action.
          </p>
          <div className="actions">
            <button disabled={busy} onClick={async () => { await act("/demo/seed"); }}>✨ Load demo data</button>
            <button className="link" onClick={() => goTo("Subjects")}>Add my own subjects</button>
          </div>
        </section>
      )}
      <section className="stats">
        <div className="stat"><b>{ins ? ins.streak : 0}</b><span>day streak</span></div>
        <div className="stat"><b>{ins ? ins.weekCompleted : 0}/{ins ? ins.weekPlanned : 0}</b><span>sessions this week</span></div>
        <div className="stat"><b>{ins ? Math.round(ins.minutesStudied / 60 * 10) / 10 : 0}h</b><span>studied in total</span></div>
      </section>

      <section className="card">
        <h2>Reminders</h2>
        <ul>
          {data.reminders.map((r, i) => (
            <li key={i} className={`rem ${r.severity}`}>{r.message}</li>
          ))}
          {data.reminders.length === 0 && <li className="empty">Nothing needs your attention. 🎉</li>}
        </ul>
      </section>

      <section className="card">
        <h2>Today's plan</h2>
        <ul>
          {todays.map((e) => (
            <li key={e.id} className={e.completed ? "done" : ""}>
              <span>{e.startTime} · <strong>{e.subjectName}</strong> · {e.durationMinutes} min</span>
              <span className="muted">{e.completed ? "done" : e.reason}</span>
            </li>
          ))}
          {todays.length === 0 && (
            <li className="empty">
              No sessions today.{" "}
              <button className="link" onClick={() => goTo(data.subjects.length ? "Plan" : "Subjects")}>
                {data.subjects.length ? "Generate a plan" : "Add subjects first"}
              </button>
            </li>
          )}
        </ul>
      </section>

      {ins && ins.subjects.length > 0 && (
        <section className="card">
          <h2>Where to focus</h2>
          <ul>
            {ins.subjects.slice(0, 3).map((s) => (
              <li key={s.subjectId}>
                <span><strong>{s.name}</strong> <span className="muted">priority {s.priority}</span></span>
                <span className="muted">{s.advice}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
