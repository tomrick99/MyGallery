import type { PhotoSummary } from "@/types/photo";

/**
 * assignArchiveVariants — small stable layout pattern for one month of
 * photos (docs/FRONTEND_TECHNICAL_DESIGN.md §10).
 *
 * Rules (pure, deterministic, no nth-child hacks):
 * 1. First photo of the month → "feature"
 * 2. Every 4th → "medium" (right-aligned floating anchor)
 * 3. Otherwise by orientation: landscape → "wide", portrait → "portrait",
 *    square → "medium"
 * 4. Adjacent duplicates degrade to "offset" to keep the rhythm varied
 *
 * Variants only control width/offset — never aspect ratio. The photo's own
 * aspectRatio always drives the rendered shape.
 */

export type ArchiveVariant =
  | "feature"
  | "wide"
  | "portrait"
  | "medium"
  | "offset";

export function assignArchiveVariants(
  photos: PhotoSummary[],
): ArchiveVariant[] {
  const result: ArchiveVariant[] = [];

  photos.forEach((photo, i) => {
    let variant: ArchiveVariant;

    if (i === 0) {
      variant = "feature";
    } else if (i % 4 === 3) {
      variant = "medium";
    } else if (photo.orientation === "portrait") {
      variant = "portrait";
    } else if (photo.orientation === "square") {
      variant = "medium";
    } else {
      variant = "wide";
    }

    if (i > 0 && result[i - 1] === variant) {
      variant = "offset";
    }

    result.push(variant);
  });

  return result;
}
