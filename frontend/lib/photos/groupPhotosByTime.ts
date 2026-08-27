import type { PhotoSummary } from "@/types/photo";

/**
 * groupPhotosByTime — pure view-model grouping for the Timeline Archive.
 *
 * Year → Month → Photos, all newest first:
 * - years descending
 * - months descending (12 → 1)
 * - photos within a month by takenAt descending
 *
 * Empty years/months simply never exist (no empty groups are rendered).
 * `year` / `month` are backend-derived response fields on PhotoSummary;
 * nothing here is persisted or remodeled.
 */

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
