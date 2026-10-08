import { useMemo, useState } from "react";

const emptyTask = { title: "", description: "", date: "", priority: "Medium", subjectId: "" };
const today = () => new Date().toLocaleDateString("en-CA");

export default function Tasks({ data, act, busy }) {
  const [task, setTask] = useState(emptyTask);
  const [filter, setFilter] = useState("all");
  const { subjects, tasks } = data;

  const subjectName = (id) => subjects.find((s) => s.id === id)?.name;
  const done = tasks.filter((t) => t.completed).length;
  const percent = tasks.length ? Math.round((done / tasks.length) * 100) : 0;
  const visible = useMemo(
    () =>
      tasks
        .filter((t) => filter === "all" || (filter === "done") === t.completed)
        .sort((a, b) => (a.date || "").localeCompare(b.date || "")),
    [tasks, filter]
  );

  const addTask = async (e) => {
    e.preventDefault();
    const body = { ...task, subjectId: task.subjectId ? Number(task.subjectId) : null };
    await act("/tasks", "POST", body);
    setTask(emptyTask);
  };

  return (
    <section className="card">
      <h2>Tasks</h2>
      <div className="progress" title={`${done} of ${tasks.length} done`}>
        <div style={{ width: `${percent}%` }} />
      </div>
      <p className="muted">{done} of {tasks.length} done ({percent}%). Pending tasks raise a subject's priority in your plan.</p>
      <div className="filters">
        {["all", "pending", "done"].map((f) => (
          <button key={f} type="button" className={filter === f ? "" : "link"} onClick={() => setFilter(f)}>
            {f[0].toUpperCase() + f.slice(1)}
          </button>
        ))}
      </div>
      <form className="row" onSubmit={addTask}>
        <input placeholder="Title" value={task.title} onChange={(e) => setTask({ ...task, title: e.target.value })} required />
        <input placeholder="Description" value={task.description} onChange={(e) => setTask({ ...task, description: e.target.value })} />
        <input type="date" value={task.date} onChange={(e) => setTask({ ...task, date: e.target.value })} required />
        <select value={task.subjectId} onChange={(e) => setTask({ ...task, subjectId: e.target.value })}>
          <option value="">No subject</option>
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
        <select value={task.priority} onChange={(e) => setTask({ ...task, priority: e.target.value })}>
          <option>High</option>
          <option>Medium</option>
          <option>Low</option>
        </select>
        <button type="submit" disabled={busy}>Add</button>
      </form>
      <ul>
        {visible.map((t) => (
          <li key={t.id} className={(t.completed ? "done " : "") + (!t.completed && t.date && t.date < today() ? "overdue" : "")}>
            <label>
              <input type="checkbox" checked={t.completed} onChange={() => act(`/tasks/${t.id}/toggle`, "PUT")} />{" "}
              <strong>{t.title}</strong> {t.description} — {t.date}{" "}
              <span className={`tag ${t.priority}`}>{t.priority}</span>
              {subjectName(t.subjectId) && <span className="tag subj">{subjectName(t.subjectId)}</span>}
            </label>
            <button className="link" disabled={busy} onClick={() => act(`/tasks/${t.id}`, "DELETE")}>Delete</button>
          </li>
        ))}
        {visible.length === 0 && <li className="empty">No tasks to show.</li>}
      </ul>
    </section>
  );
}
