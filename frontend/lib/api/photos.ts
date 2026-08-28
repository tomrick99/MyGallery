/**
 * Public Photo API module — server-side only.
 *
 * Consumed exclusively by Server Components (app/page.tsx). The base URL
 * comes from the server-only API_BASE_URL env; nothing here ever runs in
 * the browser. Cache policy is centralized: both reads revalidate every
 * 300 seconds through the Next.js Data Cache.
 */

import type { PhotoSummary } from "@/types/photo";
import type { ArchiveYear } from "@/types/archive";
import { apiFetch } from "./client";

const REVALIDATE_SECONDS = 300;

/** Featured pool — drives both the Hero and the Selected Frames stream. */
export function getFeaturedPhotos(): Promise<PhotoSummary[]> {
  return apiFetch<PhotoSummary[]>("/api/v1/photos/featured", {
    next: { revalidate: REVALIDATE_SECONDS },
  });
}

/** Timeline archive — already grouped Year → Month → Photos by the backend. */
export function getArchive(): Promise<ArchiveYear[]> {
  return apiFetch<ArchiveYear[]>("/api/v1/archive", {
    next: { revalidate: REVALIDATE_SECONDS },
  });
}
