import { useState } from "react";
import { api } from "./api";
import AuthLayout from "./AuthLayout";

export default function Login({ onLogin, onSwitch, notice }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      onLogin(await api("/login", { method: "POST", body: { email, password } }));
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Log in to see today's study plan."
      footer={
        <>
          New here?{" "}
          <button type="button" className="link" onClick={onSwitch}>Create an account</button>
        </>
      }
    >
      <form className="auth-form" onSubmit={submit}>
        {notice && <p className="ok" role="status">{notice}</p>}
        {error && <p className="err" role="alert">{error}</p>}
        <label>
          Email
          <input type="email" placeholder="you@example.com" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <span className="pw">
            <input type={show ? "text" : "password"} placeholder="Your password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <button type="button" className="link" onClick={() => setShow(!show)}>{show ? "Hide" : "Show"}</button>
          </span>
        </label>
        <button type="submit" className="primary" disabled={loading}>{loading ? "Logging in…" : "Log in"}</button>
      </form>
    </AuthLayout>
  );
}
