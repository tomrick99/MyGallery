"use client";

import Image from "next/image";
import { useState, type FormEvent } from "react";
import type { AdminPhoto } from "@/types/admin";
import { AdminApiError, updatePhoto } from "@/lib/api/admin-client";
import { describeAdminError } from "./errors";
import {
  buildMetadataPayload,
  mapBackendFieldErrors,
  metadataValuesFromPhoto,
  validateMetadata,
  type MetadataErrors,
} from "./metadataForm";
import AdminModal from "./AdminModal";
import { Field, TextArea, TextInput } from "./fields";
import styles from "./admin.module.css";

/**
 * AdminPhotoEditor — full-replacement metadata editing.
 * Required (title, takenAt, featured, visibility) are always sent; blank
 * optionals become null. featured and visibility are independent —
 * featured=true + PRIVATE is valid.
 */
export default function AdminPhotoEditor({
  photo,
  onClose,
  onSaved,
  onDeleteRequest,
}: {
  photo: AdminPhoto;
  onClose: () => void;
  onSaved: () => void;
  onDeleteRequest: (photo: AdminPhoto) => void;
}) {
  const [values, setValues] = useState(() => metadataValuesFromPhoto(photo));
  const [errors, setErrors] = useState<MetadataErrors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set =
    <K extends keyof typeof values>(key: K) =>
    (value: (typeof values)[K]) =>
      setValues((current) => ({ ...current, [key]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy) return;

    const clientErrors = validateMetadata(values);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    setBusy(true);
    setError(null);
    try {
      await updatePhoto(photo.id, buildMetadataPayload(values));
      onSaved();
    } catch (err) {
      if (err instanceof AdminApiError && err.fieldErrors) {
        setErrors(mapBackendFieldErrors(err.fieldErrors));
      }
      setError(describeAdminError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AdminModal label={`Edit ${photo.title}`} busy={busy} onClose={onClose} wide>
      <form className={styles.editor} onSubmit={submit}>
        <div className={styles.editorPreview}>
          <Image
            src={photo.image.displayUrl}
            alt={photo.title}
            width={1024}
            height={Math.round(1024 / photo.aspectRatio)}
            unoptimized
            className={styles.editorImage}
          />
          <p className={styles.editorMeta}>
            {photo.year} · {photo.orientation} · {photo.width}×{photo.height}
          </p>
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
            <TextArea rows={4} value={values.description} onChange={set("description")} maxLength={5000} />
          </Field>

          {error ? <p className={styles.errorBanner}>{error}</p> : null}

          <div className={styles.editorActions}>
            <button type="submit" className={styles.primaryButton} disabled={busy}>
              {busy ? "Saving…" : "Save Changes"}
            </button>
            <button type="button" className={styles.ghostButton} onClick={onClose} disabled={busy}>
              Cancel
            </button>
            <button
              type="button"
              className={styles.dangerButton}
              onClick={() => onDeleteRequest(photo)}
              disabled={busy}
            >
              Delete
            </button>
          </div>
        </div>
      </form>
    </AdminModal>
  );
}
