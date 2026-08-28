/**
 * Shared metadata form state helpers for the admin editor and upload panel.
 * Pure functions — React components own the useState themselves.
 */

import type {
  AdminPhoto,
  PhotoMetadataInput,
  Visibility,
} from "@/types/admin";
import {
  parseAperture,
  parseFocalLength,
  parseShutterSpeed,
  toInputValue,
} from "@/lib/admin/exposure";

export interface MetadataFormValues {
  title: string;
  takenAt: string;
  location: string;
  featured: boolean;
  visibility: Visibility;
  camera: string;
  lens: string;
  focalLengthMm: string;
  aperture: string;
  shutterSpeedSeconds: string;
  iso: string;
  description: string;
}

export function emptyMetadataValues(): MetadataFormValues {
  return {
    title: "",
    takenAt: "",
    location: "",
    featured: false,
    visibility: "PRIVATE", // safe default
    camera: "",
    lens: "",
    focalLengthMm: "",
    aperture: "",
    shutterSpeedSeconds: "",
    iso: "",
    description: "",
  };
}

export function metadataValuesFromPhoto(photo: AdminPhoto): MetadataFormValues {
  return {
    title: photo.title,
    takenAt: photo.takenAt,
    location: photo.location ?? "",
    featured: photo.featured,
    visibility: photo.visibility,
    camera: photo.camera ?? "",
    lens: photo.lens ?? "",
    focalLengthMm: toInputValue(parseFocalLength(photo.focalLength)),
    aperture: toInputValue(parseAperture(photo.aperture)),
    shutterSpeedSeconds: toInputValue(parseShutterSpeed(photo.shutterSpeed)),
    iso: photo.iso === null ? "" : String(photo.iso),
    description: photo.description ?? "",
  };
}

export type MetadataErrors = Partial<Record<keyof MetadataFormValues, string>>;

function isPositiveNumber(value: string): boolean {
  const n = Number(value);
  return !Number.isNaN(n) && n > 0;
}

/** UX-level validation only — the backend remains authoritative. */
export function validateMetadata(values: MetadataFormValues): MetadataErrors {
  const errors: MetadataErrors = {};
  const today = new Date().toISOString().slice(0, 10);

  if (!values.title.trim()) errors.title = "Title is required.";
  else if (values.title.trim().length > 200) errors.title = "Max 200 characters.";

  if (!values.takenAt) errors.takenAt = "Capture date is required.";
  else if (values.takenAt > today) errors.takenAt = "Date cannot be in the future.";

  if (values.location.trim().length > 200) errors.location = "Max 200 characters.";
  if (values.camera.trim().length > 150) errors.camera = "Max 150 characters.";
  if (values.lens.trim().length > 200) errors.lens = "Max 200 characters.";
  if (values.description.trim().length > 5000)
    errors.description = "Max 5000 characters.";

  if (values.focalLengthMm.trim() && !isPositiveNumber(values.focalLengthMm))
    errors.focalLengthMm = "Must be a positive number.";
  if (values.aperture.trim() && !isPositiveNumber(values.aperture))
    errors.aperture = "Must be a positive number.";
  if (values.shutterSpeedSeconds.trim() && !isPositiveNumber(values.shutterSpeedSeconds))
    errors.shutterSpeedSeconds = "Must be a positive number.";
  if (values.iso.trim()) {
    const n = Number(values.iso);
    if (!Number.isInteger(n) || n <= 0) errors.iso = "Must be a positive integer.";
  }

  return errors;
}

const blankToNull = (value: string): string | null => {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
};

const numberOrNull = (value: string): number | null =>
  value.trim() === "" ? null : Number(value);

/** Blank optional fields become null; required fields always present. */
export function buildMetadataPayload(
  values: MetadataFormValues,
): PhotoMetadataInput {
  return {
    title: values.title.trim(),
    takenAt: values.takenAt,
    location: blankToNull(values.location),
    featured: values.featured,
    visibility: values.visibility,
    camera: blankToNull(values.camera),
    lens: blankToNull(values.lens),
    focalLengthMm: numberOrNull(values.focalLengthMm),
    aperture: numberOrNull(values.aperture),
    shutterSpeedSeconds: numberOrNull(values.shutterSpeedSeconds),
    iso: values.iso.trim() === "" ? null : Number.parseInt(values.iso, 10),
    description: blankToNull(values.description),
  };
}

/** Maps backend fieldErrors onto form fields for display. */
export function mapBackendFieldErrors(
  fieldErrors: { field: string; message: string }[],
): MetadataErrors {
  const mapped: MetadataErrors = {};
  for (const error of fieldErrors) {
    if (error.field in emptyMetadataValues()) {
      mapped[error.field as keyof MetadataFormValues] = error.message;
    }
  }
  return mapped;
}
