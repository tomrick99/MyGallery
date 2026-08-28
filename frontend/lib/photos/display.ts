/**
 * Display fallbacks for photos without a title.
 *
 * Title is optional (blank is stored as null) and "Untitled" is never
 * invented. Fallback order:
 *   title → location → year (from takenAt)
 * Alt text stays meaningful for assistive technology.
 */

export interface DisplayPhoto {
  title: string | null;
  location: string | null;
  takenAt: string;
}

/** "Orange Steel Over Water — Coastal Pier" · "Coastal Pier" · "2026" */
export function photoCaptionText(photo: DisplayPhoto): string {
  if (photo.title) {
    return photo.location
      ? `${photo.title} — ${photo.location}`
      : photo.title;
  }
  if (photo.location) return photo.location;
  return photo.takenAt.slice(0, 4);
}

/** Meaningful alt: title · "Photograph — London" · "Photograph from 2026-08-28" */
export function photoAltText(photo: DisplayPhoto): string {
  if (photo.title) {
    return photo.location
      ? `${photo.title} — ${photo.location}`
      : photo.title;
  }
  if (photo.location) return `Photograph — ${photo.location}`;
  return `Photograph from ${photo.takenAt}`;
}
