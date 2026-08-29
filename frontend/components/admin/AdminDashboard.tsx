"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import type { AdminPhoto, AdminPhotoPage } from "@/types/admin";
import { listPhotos } from "@/lib/api/admin-client";
import { describeAdminError } from "./errors";
import AdminPhotoGrid from "./AdminPhotoGrid";
import AdminPhotoEditor from "./AdminPhotoEditor";
import AdminDeleteDialog from "./AdminDeleteDialog";
import AdminUploadPanel from "./AdminUploadPanel";
import styles from "./admin.module.css";

/**
 * AdminDashboard — photo list + pagination + editor/delete/upload modals.
 * PUBLIC and PRIVATE photos are both shown with restrained status labels.
 */
export default function AdminDashboard({
  username,
  logoutBusy,
  onLogout,
}: {
  username: string;
  logoutBusy: boolean;
  onLogout: () => void;
}) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPhotoPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<AdminPhoto | null>(null);
  const [deleting, setDeleting] = useState<AdminPhoto | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

  const fetchPage = useCallback(async (target: number) => {
    try {
      const result = await listPhotos(target);
      // Deleting the last item of a non-zero page empties it — step back.
      if (result.items.length === 0 && result.page > 0) {
        setPage(result.page - 1);
        return;
      }
      setData(result);
      setError(null);
    } catch (err) {
      setError(describeAdminError(err));
    }
  }, []);

  useEffect(() => {
    // Defer the fetch kickoff past the synchronous effect body so the
    // resulting state updates are not cascading renders.
    const frame = requestAnimationFrame(() => void fetchPage(page));
    return () => cancelAnimationFrame(frame);
  }, [page, fetchPage]);

  // Initial load only: during page changes the previous grid stays visible.
  const loading = data === null && error === null;

  const rangeStart =
    data && data.totalElements > 0 ? data.page * data.size + 1 : 0;
  const rangeEnd = data
    ? Math.min((data.page + 1) * data.size, data.totalElements)
    : 0;

  return (
    <div className={styles.dashboard}>
      <header className={styles.dashboardHeader}>
        <div>
          <p className={styles.loginKicker}>MYGALLERY</p>
          <h1 className={styles.dashboardTitle}>ARCHIVE MANAGEMENT</h1>
          <p className={styles.dashboardUser}>{username}</p>
        </div>
        <nav className={styles.dashboardActions} aria-label="Admin actions">
          <Link href="/" className={styles.ghostButton}>
            View Site
          </Link>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => setUploadOpen(true)}
          >
            Upload Photo
          </button>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onLogout}
            disabled={logoutBusy}
          >
            {logoutBusy ? "Signing out…" : "Logout"}
          </button>
        </nav>
      </header>

      {error ? <p className={styles.errorBanner}>{error}</p> : null}

      {loading ? (
        <p className={styles.stateText}>Loading photos…</p>
      ) : !data || data.items.length === 0 ? (
        <div className={styles.emptyState}>
          <p className={styles.stateText}>No photos yet.</p>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => setUploadOpen(true)}
          >
            Upload Photo
          </button>
        </div>
      ) : (
        <>
          <AdminPhotoGrid photos={data.items} onEdit={setEditing} />
          <div className={styles.pagination}>
            <button
              type="button"
              className={styles.ghostButton}
              disabled={data.page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <span className={styles.paginationInfo}>
              {rangeStart}–{rangeEnd} of {data.totalElements}
            </span>
            <button
              type="button"
              className={styles.ghostButton}
              disabled={data.page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}

      {editing ? (
        <AdminPhotoEditor
          photo={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            void fetchPage(page);
          }}
          onDeleteRequest={(photo) => {
            setEditing(null);
            setDeleting(photo);
          }}
        />
      ) : null}

      {deleting ? (
        <AdminDeleteDialog
          photo={deleting}
          onCancel={() => setDeleting(null)}
          onDeleted={() => {
            setDeleting(null);
            void fetchPage(page);
          }}
        />
      ) : null}

      {uploadOpen ? (
        <AdminUploadPanel
          onClose={() => setUploadOpen(false)}
          onSaved={() => {
            setPage(0);
            void fetchPage(0);
          }}
        />
      ) : null}
    </div>
  );
}
