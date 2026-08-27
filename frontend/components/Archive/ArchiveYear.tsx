import type { ArchiveYear as ArchiveYearData } from "@/lib/photos/groupPhotosByTime";
import ArchiveMonth from "./ArchiveMonth";
import styles from "./Archive.module.css";

export default function ArchiveYear({ year }: { year: ArchiveYearData }) {
  return (
    <section className={styles.year} aria-label={`${year.year}`}>
      <h2 className={styles.yearLabel}>
        {year.year}
        <small className={styles.yearCount}>{year.photoCount} PHOTOS</small>
      </h2>
      {year.months.map((month) => (
        <ArchiveMonth key={month.month} month={month} />
      ))}
    </section>
  );
}
