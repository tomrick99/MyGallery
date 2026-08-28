/**
 * MyGallery — archive response contract.
 * Mirrors the backend `GET /api/v1/archive` payload
 * (docs/DATA_MODEL.md §10). API/view response types, not domain entities.
 */

import type { PhotoSummary } from "./photo";

export interface ArchiveMonth {
  month: number;
  label: string;
  photoCount: number;
  photos: PhotoSummary[];
}

export interface ArchiveYear {
  year: number;
  photoCount: number;
  months: ArchiveMonth[];
}
