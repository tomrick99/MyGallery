/**
 * Local typed mock photo data.
 *
 * Used only until the Spring Boot Photo API exists. The shape follows the
 * formal contract (types/photo.ts); swapping the source later means replacing
 * `mockFeaturedPhotos` with `getFeaturedPhotos()` from lib/api — consumers
 * (e.g. Hero) will not change.
 *
 * Mock-only simplification: all three image variants point at the same
 * development asset in public/dev/.
 */

import type { PhotoImage, PhotoSummary } from "@/types/photo";

function devImage(fileName: string): PhotoImage {
  const url = `/dev/${fileName}`;
  return { thumbnailUrl: url, cardUrl: url, displayUrl: url };
}

export const mockPhotos: PhotoSummary[] = [
  {
    id: "mock-001",
    title: "Orange Steel Over Water",
    takenAt: "2026-08-14",
    year: 2026,
    month: 8,
    location: "Coastal Pier",
    orientation: "landscape",
    aspectRatio: 1.5,
    featured: true,
    image: devImage("490C7F5F-95A8-4B29-AC35-F4F537580C82_1_105_c.jpeg"),
  },
  {
    id: "mock-002",
    title: "Blue Hour Crossing",
    takenAt: "2026-08-09",
    year: 2026,
    month: 8,
    location: "Haihe River",
    orientation: "portrait",
    aspectRatio: 0.667,
    featured: true,
    image: devImage("D92E7AD0-4EFA-4196-8898-616D25C62A5D_1_105_c.jpeg"),
  },
  {
    id: "mock-003",
    title: "Two Figures, Magenta Arc",
    takenAt: "2026-08-02",
    year: 2026,
    month: 8,
    location: "Daguangming Bridge",
    orientation: "portrait",
    aspectRatio: 0.563,
    featured: true,
    image: devImage("4619B44C-48E5-4B94-9FE9-95E8DC716936_1_105_c.jpeg"),
  },
  {
    id: "mock-004",
    title: "Tetrapod Smile",
    takenAt: "2026-07-21",
    year: 2026,
    month: 7,
    location: "Breakwater",
    orientation: "landscape",
    aspectRatio: 1.777,
    featured: true,
    image: devImage("60BF2596-73C4-435F-8EA6-66B529615532_1_105_c.jpeg"),
  },
  {
    id: "mock-005",
    title: "Dashboard Companion",
    takenAt: "2026-07-12",
    year: 2026,
    month: 7,
    location: null,
    orientation: "landscape",
    aspectRatio: 1.775,
    featured: false,
    image: devImage("141C83E6-BA7B-420A-BEA6-DC6C4A344755_1_105_c.jpeg"),
  },
  {
    id: "mock-006",
    title: "Rust Geometry II",
    takenAt: "2025-11-18",
    year: 2025,
    month: 11,
    location: "Coastal Pier",
    orientation: "landscape",
    aspectRatio: 1.5,
    featured: false,
    image: devImage("490C7F5F-95A8-4B29-AC35-F4F537580C82_1_105_c.jpeg"),
  },
  {
    id: "mock-007",
    title: "Neon Suspension",
    takenAt: "2025-11-03",
    year: 2025,
    month: 11,
    location: "Haihe River",
    orientation: "portrait",
    aspectRatio: 0.667,
    featured: false,
    image: devImage("D92E7AD0-4EFA-4196-8898-616D25C62A5D_1_105_c.jpeg"),
  },
  {
    id: "mock-008",
    title: "Concrete Shore Study",
    takenAt: "2025-03-22",
    year: 2025,
    month: 3,
    location: "Breakwater",
    orientation: "landscape",
    aspectRatio: 1.777,
    featured: false,
    image: devImage("60BF2596-73C4-435F-8EA6-66B529615532_1_105_c.jpeg"),
  },
  {
    id: "mock-009",
    title: "City Veins at Night",
    takenAt: "2024-12-15",
    year: 2024,
    month: 12,
    location: "Daguangming Bridge",
    orientation: "portrait",
    aspectRatio: 0.563,
    featured: false,
    image: devImage("4619B44C-48E5-4B94-9FE9-95E8DC716936_1_105_c.jpeg"),
  },
];

export const mockFeaturedPhotos: PhotoSummary[] = mockPhotos.filter(
  (photo) => photo.featured,
);
