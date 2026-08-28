"use client";

import { useEffect, useState, type FormEvent } from "react";
import type { UploadSignature } from "@/types/admin";
import {
  AdminApiError,
  createPhoto,
  requestUploadSignature,
  uploadToCloudinary,
} from "@/lib/api/admin-client";
import { describeAdminError } from "./errors";
import {
  buildMetadataPayload,
  emptyMetadataValues,
  mapBackendFieldErrors,
  validateMetadata,
  type MetadataErrors,
  type MetadataFormValues,
} from "./metadataForm";
import AdminModal from "./AdminModal";
import { Field, TextArea, TextInput } from "./fields";
import styles from "./admin.module.css";

/**
 * AdminUploadPanel — direct Cloudinary upload flow:
 *
 *   file + metadata (validated locally)
 *   → POST /admin/uploads/signature      (only when Save is pressed)
 *   → browser → Cloudinary direct upload (XHR, real 0–100% progress)
 *   → POST /admin/photos                 (Spring verifies the asset)
 *
 * If the Cloudinary upload succeeded but create fails, the signed publicId
 * is kept in memory and "Save" retries ONLY the create — no duplicate asset
 * is uploaded. Defaults: featured=false, visibility=PRIVATE.
 */

const MAX_BYTES = 52_428_800; // 50 MiB, backend remains final authority

const ACCEPTED_MIME = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/heic",
  "image/heif",
]);

const EXTENSION_MIME: Record<string, string> = {
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  webp: "image/webp",
  heic: "image/heic",
  heif: "image/heif",
};

function resolveMimeType(file: File): string | null {
  if (ACCEPTED_MIME.has(file.type)) return file.type;
  if (file.type === "") {
    const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
    return EXTENSION_MIME[extension] ?? null;
  }
  return null;
}

function isSignatureExpiring(signature: UploadSignature): boolean {
  const parsed = Date.parse(signature.expiresAt);
  const expiresMs = Number.isNaN(parsed)
    ? Number(signature.expiresAt) * 1000
    : parsed;
  if (Number.isNaN(expiresMs)) return false;
  return expiresMs - Date.now() < 30_000;
}

type Phase = "form" | "preparing" | "uploading" | "verifying";

const PHASE_LABEL: Record<Exclude<Phase, "form">, string> = {
  preparing: "Preparing…",
  uploading: "Uploading",
  verifying: "Verifying…",
};

export default function AdminUploadPanel({
  onClose,
  onSaved,
}: {
  onClose: () => void;
  onSaved: () => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewFailed, setPreviewFailed] = useState(false);
  const [values, setValues] = useState<MetadataFormValues>(emptyMetadataValues);
  const [errors, setErrors] = useState<MetadataErrors>({});
  const [phase, setPhase] = useState<Phase>("form");
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [uploadedPublicId, setUploadedPublicId] = useState<string | null>(null);

  const busy = phase !== "form";

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const set =
    <K extends keyof MetadataFormValues>(key: K) =>
    (value: MetadataFormValues[K]) =>
      setValues((current) => ({ ...current, [key]: value }));

  const onFileChange = (next: File | null) => {
    setError(null);
    setUploadedPublicId(null);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setPreviewFailed(false);

    if (!next) {
      setFile(null);
      return;
    }
    if (!resolveMimeType(next)) {
      setFile(null);
      setError("Unsupported file type. Use JPEG, PNG, WebP, HEIC or HEIF.");
      return;
    }
    if (next.size > MAX_BYTES) {
      setFile(null);
      setError("File exceeds the 50 MiB limit.");
      return;
    }
    setFile(next);
    setPreviewUrl(URL.createObjectURL(next));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy) return;

    const clientErrors = validateMetadata(values);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    if (!file && !uploadedPublicId) {
      setError("Choose a photo file first.");
      return;
    }

    setError(null);

    try {
      let publicId = uploadedPublicId;

      if (!publicId && file) {
        const mime = resolveMimeType(file);
        if (!mime) {
          setError("Unsupported file type.");
          return;
        }

        setPhase("preparing");
        const signatureRequest = {
          fileName: file.name,
          contentType: mime,
          bytes: file.size,
        };
        let signature = await requestUploadSignature(signatureRequest);
        if (isSignatureExpiring(signature)) {
          signature = await requestUploadSignature(signatureRequest);
        }

        setPhase("uploading");
        setProgress(0);
        await uploadToCloudinary(signature, file, setProgress);
        publicId = signature.publicId;
        setUploadedPublicId(publicId);
      }

      if (!publicId) {
        setError("Choose a photo file first.");
        return;
      }

      setPhase("verifying");
      await createPhoto({
        ...buildMetadataPayload(values),
        cloudinaryPublicId: publicId,
      });

      if (previewUrl) URL.revokeObjectURL(previewUrl);
      onSaved();
    } catch (err) {
      setPhase("form");
      if (err instanceof AdminApiError && err.fieldErrors) {
        setErrors(mapBackendFieldErrors(err.fieldErrors));
      }
      setError(describeAdminError(err));
    }
  };

  const fileSummary = file
    ? `${file.name} · ${resolveMimeType(file) ?? "unknown"} · ${(file.size / 1_048_576).toFixed(1)} MB`
    : null;

  return (
    <AdminModal label="Upload photo" busy={busy} onClose={onClose} wide>
      <form className={styles.editor} onSubmit={submit}>
        <div className={styles.editorPreview}>
          {previewUrl && !previewFailed ? (
            // Local object-URL preview; revoked on close/success.
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={previewUrl}
              alt="Selected file preview"
              className={styles.editorImage}
              onError={() => setPreviewFailed(true)}
            />
          ) : (
            <div className={styles.uploadPlaceholder}>
              {fileSummary ?? "No file selected"}
            </div>
          )}
          <Field label="File (JPEG / PNG / WebP / HEIC / HEIF, ≤ 50 MiB)">
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.webp,.heic,.heif"
              className={styles.fileInput}
              disabled={busy || uploadedPublicId !== null}
              onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
            />
          </Field>
          {uploadedPublicId ? (
            <p className={styles.uploadedNote}>
              File uploaded — fix metadata and save again if needed. No
              duplicate will be uploaded.
            </p>
          ) : null}
        </div>

        <div className={styles.editorFields}>
          <Field label="Title" error={errors.title}>
            <TextInput value={values.title} onChange={set("title")} required maxLength={200} />
          </Field>
          <Field label="Taken At" error={errors.takenAt}>
            <TextInput type="date" value={values.takenAt} onChange={set("takenAt")} required />
          </Field>
          <Field label="Location" error={errors.location}>
            <TextInput value={values.location} onChange={set("location")} maxLength={200} />
          </Field>

          <div className={styles.inlineFields}>
            <label className={styles.checkboxField}>
              <input
                type="checkbox"
                checked={values.featured}
                onChange={(e) => set("featured")(e.target.checked)}
              />
              <span>Featured</span>
            </label>
            <Field label="Visibility">
              <select
                className={styles.input}
                value={values.visibility}
                onChange={(e) => set("visibility")(e.target.value as "PUBLIC" | "PRIVATE")}
              >
                <option value="PRIVATE">PRIVATE</option>
                <option value="PUBLIC">PUBLIC</option>
              </select>
            </Field>
          </div>

          <Field label="Camera" error={errors.camera}>
            <TextInput value={values.camera} onChange={set("camera")} maxLength={150} />
          </Field>
          <Field label="Lens" error={errors.lens}>
            <TextInput value={values.lens} onChange={set("lens")} maxLength={200} />
          </Field>

          <div className={styles.inlineFields}>
            <Field label="Focal Length (mm)" error={errors.focalLengthMm}>
              <TextInput inputMode="decimal" value={values.focalLengthMm} onChange={set("focalLengthMm")} placeholder="35" />
            </Field>
            <Field label="Aperture" error={errors.aperture}>
              <TextInput inputMode="decimal" value={values.aperture} onChange={set("aperture")} placeholder="2.8" />
            </Field>
          </div>
          <div className={styles.inlineFields}>
            <Field label="Shutter (s)" error={errors.shutterSpeedSeconds}>
              <TextInput inputMode="decimal" value={values.shutterSpeedSeconds} onChange={set("shutterSpeedSeconds")} placeholder="0.004" />
            </Field>
            <Field label="ISO" error={errors.iso}>
              <TextInput inputMode="numeric" value={values.iso} onChange={set("iso")} placeholder="400" />
            </Field>
          </div>

          <Field label="Description" error={errors.description}>
            <TextArea rows={3} value={values.description} onChange={set("description")} maxLength={5000} />
          </Field>

          {busy ? (
            <div className={styles.progressWrap} aria-live="polite">
              <span className={styles.progressLabel}>
                {phase === "uploading"
                  ? `${PHASE_LABEL.uploading} ${progress}%`
                  : PHASE_LABEL[phase as Exclude<Phase, "form">]}
              </span>
              <span className={styles.progressTrack}>
                <span
                  className={styles.progressBar}
                  style={{ width: `${phase === "uploading" ? progress : 100}%` }}
                />
              </span>
            </div>
          ) : null}

          {error ? <p className={styles.errorBanner}>{error}</p> : null}

          <div className={styles.editorActions}>
            <button type="submit" className={styles.primaryButton} disabled={busy}>
              {uploadedPublicId ? "Retry Save" : busy ? "Working…" : "Upload & Save"}
            </button>
            <button type="button" className={styles.ghostButton} onClick={onClose} disabled={busy}>
              Cancel
            </button>
          </div>
        </div>
      </form>
    </AdminModal>
  );
}
