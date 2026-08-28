/**
 * Narrow parsers for the frozen backend display formats of exposure values
 * (docs/DATA_MODEL.md §2.3): admin responses format them for display, while
 * PUT/POST expect numerics. Scoped to exactly these three formats.
 */

/** "35 mm" → 35 */
export function parseFocalLength(value: string | null): number | null {
  if (!value) return null;
  const match = value.trim().match(/^([\d.]+)\s*mm$/i);
  return match ? Number(match[1]) : null;
}

/** "f/2.8" → 2.8 */
export function parseAperture(value: string | null): number | null {
  if (!value) return null;
  const match = value.trim().match(/^f\/([\d.]+)$/i);
  return match ? Number(match[1]) : null;
}

/** "1/250 s" → 0.004 · "2 s" → 2 */
export function parseShutterSpeed(value: string | null): number | null {
  if (!value) return null;
  const trimmed = value.trim();
  const fraction = trimmed.match(/^1\/([\d.]+)\s*s$/i);
  if (fraction) return 1 / Number(fraction[1]);
  const seconds = trimmed.match(/^([\d.]+)\s*s$/i);
  return seconds ? Number(seconds[1]) : null;
}

/** number | null → editable input string ("" for null). */
export function toInputValue(value: number | null): string {
  return value === null ? "" : String(value);
}
