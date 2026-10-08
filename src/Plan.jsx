import { useState } from "react";

const FEEDBACK = [
  ["TOO_EASY", "Too easy"],
  ["JUST_RIGHT", "Just right"],
  ["TOO_HARD", "Too hard"],
];

function dayLabel(date, today) {
  const d = new Date(date + "T00:00:00");
  const name = d.toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "short" });
  if (date === today) return `Today · ${name}`;
  return name;
}

export default function Plan({ data, act, busy, user }) {
  const [openId, setOpenId] = useState(null);
  const today = new Date().toLocaleDateString("en-CA");
  const days = [...new Set(data.plan.map((e) => e.date))];

  const complete = async (id, feedback) => {
    setOpenId(null);
    await act(`/plan/${id}/complete`, "PUT", { feedback });
  };

  return (
    <>
      <section className="card">
        <div className="between">
          <div>
            <h2>Your 7-day study plan</h2>
            <p className="muted">
              Built from your subjects, difficulty, exam dates, test scores and feedback.
              {" "}Budget: {user.dailyHours ?? 2}h/day, {(user.preferredSlot || "Evening").toLowerCase()} sessions.
            </p>
          </div>
          <button disabled={busy || data.subjects.length === 0} onClick={() => act("/plan/generate")}>
            {data.plan.length ? "Regenerate plan" : "Generate plan"}
          </button>
        </div>
        {data.subjects.length === 0 && <p className="muted">Add at least one subject first.</p>}
      </section>

      <section className="card">
        <h2>🧠 How the AI decides</h2>
        <p className="muted">
          priority = (0.25 × difficulty + 0.35 × weakness + 0.25 × exam urgency + 0.15 × pending tasks) × feedback weight.
          Weakness comes from your recent test scores (newer scores count more). Each day's study time is shared out
          in proportion to priority, and your "Too easy / Too hard" feedback re-balances the coming days.
        </p>
      </section>

      {days.map((date) => (
        <section className="card" key={date}>
          <h2>{dayLabel(date, today)}</h2>
          <ul>
            {data.plan.filter((e) => e.date === date).map((e) => (
              <li key={e.id} className={e.completed ? "done" : ""}>
                <div>
                  <strong>{e.startTime} · {e.subjectName}</strong> · {e.durationMinutes} min
                  <div className="muted">Why: {e.reason}{e.tip ? ` · Tip: ${e.tip}` : ""}</div>
                </div>
                <div className="actions">
                  {e.completed ? (
                    <>
                      <span className="tag">{FEEDBACK.find((f) => f[0] === e.feedback)?.[1] || "done"}</span>
                      <button className="link" disabled={busy} onClick={() => act(`/plan/${e.id}/undo`, "PUT")}>Undo</button>
                    </>
                  ) : openId === e.id ? (
                    <>
                      <span className="muted">How was it?</span>
                      {FEEDBACK.map(([value, label]) => (
                        <button key={value} disabled={busy} onClick={() => complete(e.id, value)}>{label}</button>
                      ))}
                    </>
                  ) : (
                    <button disabled={busy} onClick={() => setOpenId(e.id)}>Mark done</button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </>
  );
}
