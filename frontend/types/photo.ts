/**
 * MyGallery — public photo contract.
 * Mirrors the backend response DTOs (docs/DATA_MODEL.md §9.2).
 * Only the shapes the frontend consumes; `visibility` / `cloudinaryPublicId`
 * / original URLs never appear here.
 */

export interface PhotoImage {
  thumbnailUrl: string;
  cardUrl: string;
  displayUrl: string;
}

export interface PhotoSummary {
  id: string;
  title: string;
  takenAt: string;
  year: number;
  month: number;
  location: string | null;
  orientation: "landscape" | "portrait" | "square";
  aspectRatio: number;
  featured: boolean;
  image: PhotoImage;
}
