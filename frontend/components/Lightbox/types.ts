/**
 * Minimal serializable photo payload for the Lightbox client boundary.
 * Never carries cloudinaryPublicId, visibility, or original URLs.
 */
export interface LightboxPhoto {
  id: string;
  title: string;
  location: string | null;
  takenAt: string;
  aspectRatio: number;
  displayUrl: string;
}
