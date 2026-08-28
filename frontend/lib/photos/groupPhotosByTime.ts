import type { PhotoSummary } from "@/types/photo";
import type { ArchiveMonth, ArchiveYear } from "@/types/archive";

/**
 * groupPhotosByTime — pure view-model grouping helper.
 *
 * Retained only as a development/mock utility (the production homepage
 * consumes the backend's pre-grouped `GET /api/v1/archive` response
 * directly). Year → Month → Photos, all newest first; empty groups never
 * exist. Types come from the shared contract in types/archive.ts — no
 * duplicate definitions.
 */

const MONTH_LABELS = [
  "JANUARY",
  "FEBRUARY",
  "MARCH",
  "APRIL",
  "MAY",
  "JUNE",
  "JULY",
  "AUGUST",
  "SEPTEMBER",
  "OCTOBER",
  "NOVEMBER",
  "DECEMBER",
] as const;

export function groupPhotosByTime(photos: PhotoSummary[]): ArchiveYear[] {
  const byYear = new Map<number, Map<number, PhotoSummary[]>>();

  for (const photo of photos) {
    let months = byYear.get(photo.year);
    if (!months) {
      months = new Map();
      byYear.set(photo.year, months);
    }
    const list = months.get(photo.month);
    if (list) {
      list.push(photo);
    } else {
      months.set(photo.month, [photo]);
    }
  }

  return [...byYear.entries()]
    .sort((a, b) => b[0] - a[0])
    .map(([year, months]) => {
      const archiveMonths: ArchiveMonth[] = [...months.entries()]
        .sort((a, b) => b[0] - a[0])
        .map(([month, list]) => ({
          month,
          label: MONTH_LABELS[month - 1],
          photoCount: list.length,
          photos: [...list].sort((a, b) =>
            b.takenAt.localeCompare(a.takenAt),
          ),
        }));

      return {
        year,
        photoCount: archiveMonths.reduce((n, m) => n + m.photoCount, 0),
        months: archiveMonths,
      };
    });
}
