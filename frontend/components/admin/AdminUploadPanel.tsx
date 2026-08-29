"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import type { Visibility } from "@/types/admin";
import {
  AdminApiError,
  createPhoto,
  requestUploadSignature,
  uploadToCloudinary,
} from "@/lib/api/admin-client";
import { readExifCaptureDate } from "@/lib/admin/exif";
import { describeAdminError } from "./errors";
import {
  createQueueItem,
  isSignatureExpiring,
  resolveMimeType,
  type QueueItem,
} from "./uploadQueue";
import AdminModal from "./AdminModal";
import styles from "./admin.module.css";

/**
 * AdminUploadPanel — batch direct-upload queue.
 *
 * Per item: signed request → browser → Cloudinary → Spring verified create.
 * At most 2 items upload concurrently. Replay-safe per item: if Cloudinary
 * succeeded but create failed, retry performs ONLY createPhoto — never a
 * second asset. Defaults for NEW uploads: featured=true, visibility=PUBLIC.
 */

const CONCURRENCY = 2;

export default function AdminUploadPanel({
  onClose,
  onSaved,
}: {
  onClose: () => void;
  /** Called after each batch run that produced creations — refresh list. */
  onSaved: () => void;
}) {
  const [items, setItems] = useState<QueueItem[]>([]);
  const [featured, setFeatured] = useState(true);
  const [visibility, setVisibility] = useState<Visibility>("PUBLIC");

  const itemsRef = useRef(items);
  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  const patch = useCallback((id: string, changes: Partial<QueueItem>) => {
    setItems((current) =>
      current.map((item) => (item.id === id ? { ...item, ...changes } : item)),
    );
  }, []);

  // Revoke every remaining preview URL on unmount.
  useEffect(() => {
    const ref = itemsRef;
    return () => {
      for (const item of ref.current) {
        if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
      }
    };
  }, []);

  const addFiles = (files: File[]) => {
    const created = files.map(createQueueItem);
    setItems((current) => [...current, ...created]);
    // EXIF capture dates fill in asynchronously; failure leaves the field
    // blank and never blocks the item.
    for (const item of created) {
      if (item.state !== "ready") continue;
      void readExifCaptureDate(item.file).then((date) => {
        if (date) {
          setItems((current) =>
            current.map((existing) =>
              existing.id === item.id && !existing.takenAt
                ? { ...existing, takenAt: date }
                : existing,
            ),
          );
        }
      });
    }
  };

  const removeItem = (id: string) => {
    const item = itemsRef.current.find((entry) => entry.id === id);
    if (item?.previewUrl) URL.revokeObjectURL(item.previewUrl);
    setItems((current) => current.filter((entry) => entry.id !== id));
  };

  const processItem = useCallback(
    async (id: string) => {
      const item = itemsRef.current.find((entry) => entry.id === id);
      if (!item || item.state === "done") return;
      if (!item.takenAt) {
        patch(id, { state: "error", error: "Taken At is required." });
        return;
      }

      try {
        let publicId = item.uploadedPublicId;

        if (!publicId) {
          const mime = resolveMimeType(item.file);
          if (!mime) {
            patch(id, { state: "error", error: "Unsupported file type." });
            return;
          }
          patch(id, { state: "preparing", error: null });
          const signatureRequest = {
            fileName: item.file.name,
            contentType: mime,
            bytes: item.file.size,
          };
          let signature = await requestUploadSignature(signatureRequest);
          if (isSignatureExpiring(signature)) {
            signature = await requestUploadSignature(signatureRequest);
          }

          patch(id, { state: "uploading", progress: 0 });
          await uploadToCloudinary(signature, item.file, (percent) =>
            patch(id, { progress: percent }),
          );
          publicId = signature.publicId;
          patch(id, { uploadedPublicId: publicId });
        }

        patch(id, { state: "verifying" });
        await createPhoto({
          title: item.title.trim() === "" ? null : item.title.trim(),
          takenAt: item.takenAt,
          location: null,
          featured,
          visibility,
          camera: null,
          lens: null,
          focalLengthMm: null,
          aperture: null,
          shutterSpeedSeconds: null,
          iso: null,
          description: null,
          cloudinaryPublicId: publicId,
        });
        patch(id, { state: "done", progress: 100, error: null });
      } catch (err) {
        patch(id, {
          state: "error",
          error:
            err instanceof AdminApiError
              ? describeAdminError(err)
              : "Upload failed.",
        });
      }
    },
    [featured, visibility, patch],
  );

  const busy = items.some(
    (item) =>
      item.state === "preparing" ||
      item.state === "uploading" ||
      item.state === "verifying",
  );

  const pending = items.filter(
    (item) => item.state === "ready" || item.state === "error",
  );
  const doneCount = items.filter((item) => item.state === "done").length;
  const errorCount = items.filter((item) => item.state === "error").length;
  const missingDates = pending.filter((item) => !item.takenAt).length;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy || pending.length === 0) return;

    const queue = pending.map((item) => item.id);
    const workers = Array.from({ length: CONCURRENCY }, async () => {
      let next = queue.shift();
      while (next) {
        await processItem(next);
        next = queue.shift();
      }
    });
    await Promise.all(workers);

    const finished = itemsRef.current.some((item) => item.state === "done");
    if (finished) onSaved();
  };

  return (
    <AdminModal label="Upload photos" busy={busy} onClose={onClose} wide>
      <form className={styles.uploadForm} onSubmit={submit}>
        <div className={styles.uploadCommon}>
          <Field fileInput onFiles={addFiles} disabled={busy} />
          <label className={styles.checkboxField}>
            <input
              type="checkbox"
              checked={featured}
              onChange={(e) => setFeatured(e.target.checked)}
              disabled={busy}
            />
            <span>Featured</span>
          </label>
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Visibility</span>
            <select
              className={styles.input}
              value={visibility}
              disabled={busy}
              onChange={(e) => setVisibility(e.target.value as Visibility)}
            >
              <option value="PUBLIC">PUBLIC</option>
              <option value="PRIVATE">PRIVATE</option>
            </select>
          </label>
        </div>

        {items.length > 0 ? (
          <ul className={styles.queue}>
            {items.map((item) => (
              <li key={item.id} className={styles.queueItem}>
                <span className={styles.queueThumb}>
                  {item.previewUrl && !item.previewFailed ? (
                    // Local object-URL preview; revoked on remove/unmount.
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={item.previewUrl}
                      alt=""
                      onError={() => patch(item.id, { previewFailed: true })}
                    />
                  ) : (
                    <span className={styles.queueThumbEmpty}>—</span>
                  )}
                </span>

                <span className={styles.queueMain}>
                  <span className={styles.queueName}>{item.file.name}</span>
                  <span className={styles.queueInputs}>
                    <input
                      type="text"
                      className={styles.input}
                      placeholder="Title (optional)"
                      maxLength={200}
                      value={item.title}
                      disabled={busy || item.state === "done"}
                      onChange={(e) => patch(item.id, { title: e.target.value })}
                      aria-label={`Title for ${item.file.name}`}
                    />
                    <input
                      type="date"
                      className={`${styles.input} ${!item.takenAt && item.state !== "done" ? styles.dateMissing : ""}`}
                      value={item.takenAt}
                      disabled={busy || item.state === "done"}
                      onChange={(e) =>
                        patch(item.id, { takenAt: e.target.value })
                      }
                      aria-label={`Taken at for ${item.file.name}`}
                      required={item.state === "ready"}
                    />
                  </span>
                  {item.state === "uploading" || item.state === "verifying" || item.state === "preparing" ? (
                    <span className={styles.queueProgress}>
                      <span
                        className={styles.queueProgressBar}
                        style={{ width: `${item.state === "uploading" ? item.progress : 100}%` }}
                      />
                    </span>
                  ) : null}
                  {item.error ? (
                    <span className={styles.queueError}>{item.error}</span>
                  ) : null}
                </span>

                <span className={styles.queueSide}>
                  <span className={styles.queueState} data-state={item.state}>
                    {item.state === "done"
                      ? "Done"
                      : item.state === "uploading"
                        ? `${item.progress}%`
                        : item.state === "error"
                          ? "Needs attention"
                          : item.state === "ready"
                            ? "Ready"
                            : "…"}
                  </span>
                  {item.state === "error" && item.uploadedPublicId ? (
                    <button
                      type="button"
                      className={styles.ghostButton}
                      disabled={busy}
                      onClick={() => void processItem(item.id)}
                    >
                      Retry Save
                    </button>
                  ) : item.state === "error" ? (
                    <button
                      type="button"
                      className={styles.ghostButton}
                      disabled={busy}
                      onClick={() => void processItem(item.id)}
                    >
                      Retry
                    </button>
                  ) : null}
                  {item.state !== "done" && !busy ? (
                    <button
                      type="button"
                      className={styles.queueRemove}
                      onClick={() => removeItem(item.id)}
                      aria-label={`Remove ${item.file.name}`}
                    >
                      Remove
                    </button>
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        ) : (
          <p className={styles.stateText}>
            Select one or more photographs to begin.
          </p>
        )}

        {items.length > 0 && (doneCount > 0 || errorCount > 0) ? (
          <p className={styles.uploadSummary} aria-live="polite">
            {doneCount} uploaded
            {errorCount > 0 ? ` · ${errorCount} needs attention` : ""}
          </p>
        ) : null}

        {missingDates > 0 ? (
          <p className={styles.queueError}>
            {missingDates} photo{missingDates === 1 ? "" : "s"} missing a
            capture date — fill the highlighted Taken At fields.
          </p>
        ) : null}

        <div className={styles.editorActions}>
          <button
            type="submit"
            className={styles.primaryButton}
            disabled={busy || pending.length === 0}
          >
            Upload {pending.length} photo{pending.length === 1 ? "" : "s"}
          </button>
          <button
            type="button"
            className={styles.ghostButton}
            onClick={onClose}
            disabled={busy}
          >
            {doneCount > 0 ? "Done" : "Cancel"}
          </button>
        </div>
      </form>
    </AdminModal>
  );
}

function Field({
  onFiles,
  disabled,
}: {
  fileInput?: boolean;
  onFiles: (files: File[]) => void;
  disabled: boolean;
}) {
  return (
    <label className={styles.field}>
      <span className={styles.fieldLabel}>
        Files (JPEG / PNG / WebP / HEIC / HEIF, ≤ 50 MiB each)
      </span>
      <input
        type="file"
        multiple
        accept=".jpg,.jpeg,.png,.webp,.heic,.heif"
        className={styles.fileInput}
        disabled={disabled}
        onChange={(e) => {
          onFiles(Array.from(e.target.files ?? []));
          e.target.value = "";
        }}
      />
    </label>
  );
}
