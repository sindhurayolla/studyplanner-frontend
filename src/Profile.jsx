import { useState } from "react";
import { api } from "./api";

const SLOTS = ["Morning", "Afternoon", "Evening", "Night"];
const STYLES = ["", "Visual", "Reading/Writing", "Practice-based", "Group discussion"];

export default function Profile({ user, data, act, onUserUpdate }) {
  const [form, setForm] = useState({
    goal: user.goal || "",
    dailyHours: user.dailyHours ?? 2,
    preferredSlot: user.preferredSlot || "Evening",
    learningStyle: user.learningStyle || "",
  });
  const [msg, setMsg] = useState("");

  const save = async (e) => {
    e.preventDefault();
    setMsg("");
    try {
      const updated = await api("/profile", {
        method: "PUT",
        userId: user.id,
        body: { ...form, dailyHours: Number(form.dailyHours) },
      });
      onUserUpdate(updated);
      await act("/plan/generate"); // preferences changed -> rebuild the plan
      setMsg("Saved. Your plan was updated.");
    } catch (err) {
      setMsg(err.message);
    }
  };

  const ins = data.insights;
  const tasksDone = data.tasks.filter((t) => t.completed).length;
  const badges = [
    [ins && ins.streak >= 3, "🔥 3-day streak"],
    [ins && ins.minutesStudied >= 600, "⏱️ 10 hours studied"],
    [data.tasks.length > 0 && tasksDone === data.tasks.length, "✅ All tasks done"],
    [ins && ins.subjects.some((s) => s.avgScore != null && s.avgScore >= 85), "🏆 85%+ in a subject"],
  ].filter(([earned]) => earned);

  return (
    <>
    <section className="card">
      <h2>{user.name}</h2>
      <p className="muted">{user.email}</p>
      {user.goal && <p className="goal">🎯 {user.goal}</p>}
      <div className="stats">
        <div className="stat"><b>{data.subjects.length}</b><span>subjects</span></div>
        <div className="stat"><b>{tasksDone}/{data.tasks.length}</b><span>tasks done</span></div>
        <div className="stat"><b>{ins ? ins.streak : 0}</b><span>day streak</span></div>
        <div className="stat"><b>{ins ? Math.round(ins.minutesStudied / 6) / 10 : 0}h</b><span>studied</span></div>
      </div>
      <div>
        {badges.length === 0 && <span className="muted">Complete sessions to earn badges.</span>}
        {badges.map(([, label]) => <span className="tag" key={label}>{label}</span>)}
      </div>
    </section>
    <form className="card profile" onSubmit={save}>
      <h2>Academic profile & preferences</h2>
      <label className="field">
        Academic goal
        <input placeholder="e.g. Score 85% in semester exams" value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value })} />
      </label>
      <label className="field">
        Available study time per day (hours)
        <input type="number" min="0.5" max="12" step="0.5" value={form.dailyHours} onChange={(e) => setForm({ ...form, dailyHours: e.target.value })} required />
      </label>
      <label className="field">
        Preferred study time
        <select value={form.preferredSlot} onChange={(e) => setForm({ ...form, preferredSlot: e.target.value })}>
          {SLOTS.map((s) => <option key={s}>{s}</option>)}
        </select>
      </label>
      <label className="field">
        Learning style
        <select value={form.learningStyle} onChange={(e) => setForm({ ...form, learningStyle: e.target.value })}>
          {STYLES.map((s) => <option key={s} value={s}>{s || "Not set"}</option>)}
        </select>
      </label>
      <button type="submit">Save & update plan</button>
      {msg && <p className="ok">{msg}</p>}
    </form>
    </>
  );
}
