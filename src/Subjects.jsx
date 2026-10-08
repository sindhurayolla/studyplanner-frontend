import { useState } from "react";

const empty = { name: "", description: "", difficulty: 3, examDate: "" };

export default function Subjects({ data, act, busy }) {
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [scores, setScores] = useState({});
  const insightOf = (id) => data.insights?.subjects.find((s) => s.subjectId === id);

  const submit = async (e) => {
    e.preventDefault();
    const body = { ...form, difficulty: Number(form.difficulty), examDate: form.examDate || null };
    await act(editingId ? `/subjects/${editingId}` : "/subjects", editingId ? "PUT" : "POST", body);
    setForm(empty);
    setEditingId(null);
  };

  const edit = (s) => {
    setEditingId(s.id);
    setForm({ name: s.name, description: s.description || "", difficulty: s.difficulty, examDate: s.examDate || "" });
  };

  const logScore = async (id) => {
    const score = Number(scores[id]);
    if (scores[id] === undefined || scores[id] === "" || Number.isNaN(score)) return;
    await act(`/subjects/${id}/scores`, "POST", { score });
    setScores({ ...scores, [id]: "" });
  };

  return (
    <section className="card">
      <h2>Subjects</h2>
      <form className="row" onSubmit={submit}>
        <input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <label className="field">
          Difficulty
          <select value={form.difficulty} onChange={(e) => setForm({ ...form, difficulty: e.target.value })}>
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={n}>{n} {n === 1 ? "(easy)" : n === 5 ? "(very hard)" : ""}</option>
            ))}
          </select>
        </label>
        <label className="field">
          Exam date
          <input type="date" value={form.examDate} onChange={(e) => setForm({ ...form, examDate: e.target.value })} />
        </label>
        <button type="submit" disabled={busy}>{editingId ? "Save" : "Add"}</button>
        {editingId && (
          <button type="button" className="link" onClick={() => { setEditingId(null); setForm(empty); }}>Cancel</button>
        )}
      </form>

      <ul>
        {data.subjects.map((s) => {
          const ins = insightOf(s.id);
          return (
            <li key={s.id} className="subject">
              <div>
                <strong>{s.name}</strong> <span className="muted">{s.description}</span>
                <div className="muted">
                  Difficulty {s.difficulty}/5
                  {s.examDate ? ` · Exam ${s.examDate}` : ""}
                  {ins?.avgScore != null ? ` · Avg score ${ins.avgScore}% (${ins.trend})` : " · No scores yet"}
                </div>
              </div>
              <div className="actions">
                <input
                  className="score"
                  type="number"
                  min="0"
                  max="100"
                  placeholder="Score %"
                  value={scores[s.id] ?? ""}
                  onChange={(e) => setScores({ ...scores, [s.id]: e.target.value })}
                />
                <button disabled={busy} onClick={() => logScore(s.id)}>Log score</button>
                <button className="link" onClick={() => edit(s)}>Edit</button>
                <button className="link" disabled={busy} onClick={() => act(`/subjects/${s.id}`, "DELETE")}>Delete</button>
              </div>
            </li>
          );
        })}
        {data.subjects.length === 0 && <li className="empty">No subjects yet. Add your first one above.</li>}
      </ul>
    </section>
  );
}
