"use client";

import { useEffect, useState } from "react";
import {
  AdminApiError,
  clearAdminMemoryState,
  getSession,
  logout,
  refreshCsrf,
  setUnauthorizedHandler,
} from "@/lib/api/admin-client";
import AdminLogin from "./AdminLogin";
import AdminDashboard from "./AdminDashboard";
import styles from "./admin.module.css";

/**
 * AdminShell — the single client boundary of /admin.
 *
 * Bootstrap: CSRF handshake → session probe. The dashboard is never shown
 * before the authentication state is known. Any 401 from the admin API
 * returns the UI to the login state and wipes in-memory admin state.
 */

type Phase = "loading" | "login" | "dashboard" | "config-error";

export default function AdminShell() {
  const [phase, setPhase] = useState<Phase>("loading");
  const [username, setUsername] = useState<string | null>(null);
  const [logoutBusy, setLogoutBusy] = useState(false);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearAdminMemoryState();
      setUsername(null);
      setPhase("login");
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await refreshCsrf();
        const session = await getSession();
        if (cancelled) return;
        setUsername(session.username);
        setPhase("dashboard");
      } catch (error) {
        if (cancelled) return;
        if (
          error instanceof AdminApiError &&
          error.code === "CONFIGURATION_MISSING"
        ) {
          setPhase("config-error");
        } else {
          setPhase("login");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogout = async () => {
    if (logoutBusy) return;
    setLogoutBusy(true);
    try {
      await logout();
    } catch {
      // Even if the request fails, local admin state must not linger.
    } finally {
      clearAdminMemoryState();
      setUsername(null);
      setLogoutBusy(false);
      setPhase("login");
    }
  };

  if (phase === "loading") {
    return (
      <div className={styles.statePage}>
        <p className={styles.stateText}>Loading…</p>
      </div>
    );
  }

  if (phase === "config-error") {
    return (
      <div className={styles.statePage}>
        <p className={styles.stateTitle}>Admin is not configured.</p>
        <p className={styles.stateText}>
          NEXT_PUBLIC_ADMIN_API_BASE_URL is missing. Set it to the public
          origin of the Spring Boot API and restart the frontend.
        </p>
      </div>
    );
  }

  if (phase === "login" || username === null) {
    return (
      <AdminLogin
        onLoggedIn={(name) => {
          setUsername(name);
          setPhase("dashboard");
        }}
      />
    );
  }

  return (
    <AdminDashboard
      username={username}
      logoutBusy={logoutBusy}
      onLogout={handleLogout}
    />
  );
}
