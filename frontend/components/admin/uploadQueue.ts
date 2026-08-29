/**
 * Upload queue model + file validation for the admin batch upload panel.
 * Each selected file becomes one queue item; invalid files become error
 * items so one bad file never blocks the rest of the batch.
 */

import type { UploadSignature } from "@/types/admin";

export type QueueItemState =
  | "ready"
  | "preparing"
  | "uploading"
  | "verifying"
  | "done"
  | "error";

export interface QueueItem {
  id: string;
  file: File;
  /** Object URL for local preview — always revoked on remove/unmount. */
  previewUrl: string | null;
  previewFailed: boolean;
  title: string;
  /** EXIF auto-filled when available; required before create. */
  takenAt: string;
  state: QueueItemState;
  progress: number;
  error: string | null;
  /** Set once the Cloudinary upload succeeded — retry creates only. */
  uploadedPublicId: string | null;
}

export const MAX_BYTES = 52_428_800; // 50 MiB, backend remains final authority

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

export function resolveMimeType(file: File): string | null {
  if (ACCEPTED_MIME.has(file.type)) return file.type;
  if (file.type === "") {
    const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
    return EXTENSION_MIME[extension] ?? null;
  }
  return null;
}

export function createQueueItem(file: File): QueueItem {
  const base: QueueItem = {
    id: crypto.randomUUID(),
    file,
    previewUrl: null,
    previewFailed: false,
    title: "",
    takenAt: "",
    state: "ready",
    progress: 0,
    error: null,
    uploadedPublicId: null,
  };

  if (!resolveMimeType(file)) {
    return {
      ...base,
      state: "error",
      error: "Unsupported file type. Use JPEG, PNG, WebP, HEIC or HEIF.",
    };
  }
  if (file.size > MAX_BYTES) {
    return {
      ...base,
      state: "error",
      error: "File exceeds the 50 MiB limit.",
    };
  }
  return { ...base, previewUrl: URL.createObjectURL(file) };
}

export function isSignatureExpiring(signature: UploadSignature): boolean {
  const parsed = Date.parse(signature.expiresAt);
  const expiresMs = Number.isNaN(parsed)
    ? Number(signature.expiresAt) * 1000
    : parsed;
  if (Number.isNaN(expiresMs)) return false;
  return expiresMs - Date.now() < 30_000;
}
