import { mockPhotos } from "@/lib/mock/photos";
import { groupPhotosByTime } from "@/lib/photos/groupPhotosByTime";
import ArchiveYear from "./ArchiveYear";
import styles from "./Archive.module.css";

/**
 * ArchiveSection — Server Component.
 *
 * The Timeline Archive: Year → Month → Photos, newest first.
 * Entirely server-rendered; PhotoCard gains its thin client trigger only
 * when the Lightbox feature lands (Step 5.6).
 */
export default function ArchiveSection() {
  const years = groupPhotosByTime(mockPhotos);

  if (years.length === 0) return null;

  return (
    <section
      id="archive"
      className={styles.archive}
      aria-label="Photography archive by time"
    >
      <div className={styles.header}>
        <span>ARCHIVE BY TIME</span>
      </div>
      {years.map((year) => (
        <ArchiveYear key={year.year} year={year} />
      ))}
    </section>
  );
}
