"use client";

import { useState, type FormEvent } from "react";
import { AdminApiError, getSession, login } from "@/lib/api/admin-client";
import { Field, TextInput } from "./fields";
import styles from "./admin.module.css";

/**
 * AdminLogin — username/password only. No remember-me, no registration,
 * no OAuth. 401 shows a generic message; the password is cleared after a
 * successful login and never logged.
 */
export default function AdminLogin({
  onLoggedIn,
}: {
  onLoggedIn: (username: string) => void;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);

    try {
      await login(username, password);
      const session = await getSession();
      setPassword("");
      onLoggedIn(session.username);
    } catch (err) {
      if (err instanceof AdminApiError) {
        if (err.status === 401) {
          setError("Invalid credentials");
        } else if (err.status === 429) {
          setError(
            `Too many login attempts.${err.retryAfter ? ` Try again in ${err.retryAfter}s.` : ""}`,
          );
        } else {
          setError(err.message);
        }
      } else {
        setError("Login failed.");
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={styles.statePage}>
      <form className={styles.loginForm} onSubmit={submit}>
        <p className={styles.loginKicker}>MYGALLERY</p>
        <h1 className={styles.loginTitle}>ADMIN</h1>

        <Field label="Username">
          <TextInput
            value={username}
            onChange={setUsername}
            autoComplete="username"
            required
          />
        </Field>
        <Field label="Password" error={error ?? undefined}>
          <TextInput
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
            required
          />
        </Field>

        <button type="submit" className={styles.primaryButton} disabled={busy}>
          {busy ? "Signing in…" : "Sign In"}
        </button>
      </form>
    </div>
  );
}
