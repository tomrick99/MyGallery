/**
 * Browser-side EXIF capture-date reading for the admin upload flow.
 *
 * Priority: DateTimeOriginal → CreateDate. GPS is never read. Parse failure
 * must never block selecting or uploading a photo — it yields null and the
 * Taken At field simply stays blank for manual entry.
 *
 * The calendar date is preserved with local date getters: EXIF capture
 * times carry no trustworthy timezone, and converting would risk shifting
 * the photographed day. exifr is dynamically imported so it stays out of
 * the public bundles.
 */
export async function readExifCaptureDate(file: File): Promise<string | null> {
  try {
    const exifr = await import("exifr");
    const data: unknown = await exifr.parse(file, {
      pick: ["DateTimeOriginal", "CreateDate"],
      gps: false,
    });
    if (!data || typeof data !== "object") return null;

    const record = data as Record<string, unknown>;
    const candidate = record.DateTimeOriginal ?? record.CreateDate;
    if (!(candidate instanceof Date) || Number.isNaN(candidate.getTime())) {
      return null;
    }

    const year = candidate.getFullYear();
    const month = String(candidate.getMonth() + 1).padStart(2, "0");
    const day = String(candidate.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  } catch {
    return null;
  }
}
