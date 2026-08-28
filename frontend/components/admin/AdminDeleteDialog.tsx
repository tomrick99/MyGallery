"use client";

import { useState } from "react";
import type { AdminPhoto } from "@/types/admin";
import { deletePhoto } from "@/lib/api/admin-client";
import { describeAdminError } from "./errors";
import AdminModal from "./AdminModal";
import styles from "./admin.module.css";

/**
 * AdminDeleteDialog — explicit confirmation, never one-click destructive.
 * On provider failure (e.g. 502) the photo intentionally remains: the
 * dialog stays open with the sanitized error so the user can retry.
 */
export default function AdminDeleteDialog({
  photo,
  onCancel,
  onDeleted,
}: {
  photo: AdminPhoto;
  onCancel: () => void;
  onDeleted: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const confirm = async () => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await deletePhoto(photo.id);
      onDeleted();
    } catch (err) {
      setError(describeAdminError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AdminModal label={`Delete ${photo.title}`} busy={busy} onClose={onCancel}>
      <div className={styles.dialogBody}>
        <p className={styles.dialogTitle}>Delete “{photo.title}”?</p>
        <p className={styles.dialogText}>
          This permanently removes the managed photo and its private
          Cloudinary asset.
        </p>
        {error ? <p className={styles.errorBanner}>{error}</p> : null}
        <div className={styles.dialogActions}>
          <button
            type="button"
            className={styles.dangerButton}
            onClick={confirm}
            disabled={busy}
          >
            {busy ? "Deleting…" : "Delete Permanently"}
          </button>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onCancel}
            disabled={busy}
          >
            Cancel
          </button>
        </div>
      </div>
    </AdminModal>
  );
}
