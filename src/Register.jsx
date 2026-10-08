import { useState } from "react";
import { api } from "./api";
import AuthLayout from "./AuthLayout";

export default function Register({ onRegistered, onSwitch }) {
  const [name, setName] = useState("");
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
      await api("/register", { method: "POST", body: { name, email, password } });
      onRegistered();
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Get a study plan built around you in minutes."
      footer={
        <>
          Already registered?{" "}
          <button type="button" className="link" onClick={onSwitch}>Log in</button>
        </>
      }
    >
      <form className="auth-form" onSubmit={submit}>
        {error && <p className="err" role="alert">{error}</p>}
        <label>
          Name
          <input placeholder="Your name" autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" placeholder="you@example.com" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <span className="pw">
            <input type={show ? "text" : "password"} placeholder="Choose a password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <button type="button" className="link" onClick={() => setShow(!show)}>{show ? "Hide" : "Show"}</button>
          </span>
        </label>
        <button type="submit" className="primary" disabled={loading}>{loading ? "Creating…" : "Create account"}</button>
      </form>
    </AuthLayout>
  );
}
