/**
 * MyGallery — private admin API contract (browser side).
 * Mirrors the frozen backend AdminPhotoResponse / session / upload DTOs.
 * The admin response intentionally never exposes cloudinaryPublicId,
 * originalUrl, createdAt, or updatedAt — do not invent them.
 */

import type { PhotoImage } from "./photo";

export type Visibility = "PUBLIC" | "PRIVATE";

export interface AdminPhoto {
  id: string;
  title: string;
  takenAt: string;
  year: number;
  month: number;
  location: string | null;
  orientation: "landscape" | "portrait" | "square";
  aspectRatio: number;
  featured: boolean;
  visibility: Visibility;
  width: number;
  height: number;
  image: PhotoImage;
  camera: string | null;
  lens: string | null;
  /** Display-formatted, e.g. "35 mm" — parse before PUT. */
  focalLength: string | null;
  /** Display-formatted, e.g. "f/2.8". */
  aperture: string | null;
  /** Display-formatted, e.g. "1/250 s" or "2 s". */
  shutterSpeed: string | null;
  iso: number | null;
  description: string | null;
}

export interface AdminPhotoPage {
  items: AdminPhoto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CsrfToken {
  headerName: string;
  token: string;
}

export interface AdminSession {
  authenticated: boolean;
  username: string;
}

export interface UploadSignature {
  cloudName: string;
  apiKey: string;
  resourceType: string;
  type: string;
  publicId: string;
  uploadPreset: string;
  overwrite: boolean;
  timestamp: number;
  signature: string;
  expiresAt: string;
}

/** Mutable metadata shared by PUT (full replacement) and POST create. */
export interface PhotoMetadataInput {
  title: string;
  takenAt: string;
  location: string | null;
  featured: boolean;
  visibility: Visibility;
  camera: string | null;
  lens: string | null;
  focalLengthMm: number | null;
  aperture: number | null;
  shutterSpeedSeconds: number | null;
  iso: number | null;
  description: string | null;
}

export interface CreatePhotoInput extends PhotoMetadataInput {
  /** Server-generated, signed public id — never user editable. */
  cloudinaryPublicId: string;
}
