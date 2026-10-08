export default function Progress({ data }) {
  const ins = data.insights;
  if (!ins || ins.subjects.length === 0) {
    return <section className="card"><p className="muted">Add subjects and log test scores to see your progress analysis.</p></section>;
  }
  const maxPriority = Math.max(...ins.subjects.map((s) => s.priority), 0.01);
  return (
    <>
      <section className="stats">
        <div className="stat"><b>{ins.streak}</b><span>day streak</span></div>
        <div className="stat"><b>{ins.weekCompleted}/{ins.weekPlanned}</b><span>sessions (last 7 days)</span></div>
        <div className="stat"><b>{Math.round(ins.minutesStudied / 6) / 10}h</b><span>total studied</span></div>
      </section>
      <section className="card">
        <h2>Performance analysis</h2>
        <ul>
          {ins.subjects.map((s) => (
            <li key={s.subjectId} className="insight">
              <div className="between">
                <strong>{s.name}</strong>
                <span className="muted">
                  {s.avgScore != null ? `avg ${s.avgScore}% · ${s.trend}` : "no scores yet"}
                  {s.daysToExam != null && s.daysToExam >= 0 ? ` · exam in ${s.daysToExam}d` : ""}
                  {` · ${s.studiedMinutes} min studied`}
                </span>
              </div>
              <div className="progress" title={`priority ${s.priority}`}>
                <div style={{ width: `${(s.priority / maxPriority) * 100}%` }} />
              </div>
              <div className="muted">Priority {s.priority}{s.weak ? " · weak area" : ""}. {s.advice}</div>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
