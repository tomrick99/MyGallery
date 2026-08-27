import type { ArchiveMonth as ArchiveMonthData } from "@/lib/photos/groupPhotosByTime";
import { assignArchiveVariants } from "@/lib/photos/layoutVariant";
import PhotoCard from "./PhotoCard";
import styles from "./Archive.module.css";

export default function ArchiveMonth({ month }: { month: ArchiveMonthData }) {
  const variants = assignArchiveVariants(month.photos);

  return (
    <section className={styles.month} aria-label={month.label}>
      <h3 className={styles.monthLabel}>{month.label}</h3>
      <div className={styles.grid}>
        {month.photos.map((photo, i) => (
          <PhotoCard key={photo.id} photo={photo} variant={variants[i]} />
        ))}
      </div>
    </section>
  );
}
