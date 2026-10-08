import { useState } from "react";
import Login from "./login";
import Register from "./Register";
import Dashboard from "./dashboard";
import "./App.css";

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch {
    return null;
  }
}

function App() {
  const [user, setUser] = useState(loadUser);
  const [page, setPage] = useState("login");
  const [notice, setNotice] = useState("");

  const saveUser = (u) => {
    localStorage.setItem("user", JSON.stringify(u));
    setUser(u);
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    setUser(null);
    setPage("login");
  };

  if (user) return <Dashboard user={user} onUserUpdate={saveUser} onLogout={handleLogout} />;

  return page === "register" ? (
    <Register
      onRegistered={() => {
        setNotice("Registration successful! Please log in.");
        setPage("login");
      }}
      onSwitch={() => setPage("login")}
    />
  ) : (
    <Login
      notice={notice}
      onLogin={saveUser}
      onSwitch={() => {
        setNotice("");
        setPage("register");
      }}
    />
  );
}

export default App;
