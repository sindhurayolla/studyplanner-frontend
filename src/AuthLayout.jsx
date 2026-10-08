import "./auth.css";

const features = [
  "Priorities built from exam dates, difficulty and test scores",
  "A daily and weekly timetable that adapts to your feedback",
  "Reminders and progress tracking in one place",
];

function Logo() {
  return (
    <div className="auth-logo">
      <span className="auth-logo-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5v-15Z" />
          <path d="M4 20.5A2.5 2.5 0 0 0 6.5 23H20v-5" />
          <path d="M9 8h6M9 12h4" />
        </svg>
      </span>
      <span>Study Planner</span>
    </div>
  );
}

/** Split-screen shell shared by the Login and Register pages. */
export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="auth-page">
      <aside className="auth-brand">
        <Logo />
        <div className="auth-pitch">
          <h1>Study smarter,<br />not longer.</h1>
          <p>An AI planner that decides what to study today, and why.</p>
          <ul>
            {features.map((f) => (
              <li key={f}>
                <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
                  <circle cx="10" cy="10" r="10" fill="rgba(255,255,255,.2)" />
                  <path d="m5.8 10.4 2.7 2.7 5.7-5.9" fill="none" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                {f}
              </li>
            ))}
          </ul>
        </div>
        <div className="auth-preview" aria-hidden="true">
          <small>TODAY'S PLAN</small>
          <div><b>Mathematics</b><span>60 min</span></div>
          <em>Exam in 6 days · weak area (avg 45%)</em>
          <div><b>Physics</b><span>40 min</span></div>
        </div>
      </aside>

      <main className="auth-main">
        <div className="auth-box">
          <div className="auth-mobile-logo"><Logo /></div>
          <h2>{title}</h2>
          <p className="auth-sub">{subtitle}</p>
          {children}
          <p className="auth-foot">{footer}</p>
        </div>
      </main>
    </div>
  );
}
