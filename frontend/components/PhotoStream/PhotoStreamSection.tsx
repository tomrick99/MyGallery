import { mockFeaturedPhotos } from "@/lib/mock/photos";
import PhotoStream from "./PhotoStream";
import PhotoStreamTrack from "./PhotoStreamTrack";
import styles from "./PhotoStream.module.css";

/**
 * PhotoStreamSection — Server Component.
 *
 * Section header + data + structure. The interactive shell (PhotoStream)
 * is a thin client boundary; the photo figures (PhotoStreamTrack) are
 * server-rendered and passed through as children, so the photo list never
 * enters the client bundle.
 */
export default function PhotoStreamSection() {
  const photos = mockFeaturedPhotos;

  if (photos.length === 0) return null;

  return (
    <section className={styles.section} aria-label="Selected frames">
      <div className={styles.header}>
        <span>SELECTED FRAMES</span>
        <span className={styles.hint}>SCROLL / DRIFT →</span>
      </div>
      <PhotoStream>
        <PhotoStreamTrack photos={photos} />
      </PhotoStream>
    </section>
  );
}
