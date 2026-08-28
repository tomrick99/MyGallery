import type { ArchiveYear as ArchiveYearData } from "@/types/archive";
import ArchiveYear from "./ArchiveYear";
import styles from "./Archive.module.css";

/**
 * ArchiveSection — Server Component.
 *
 * The Timeline Archive. Consumes the backend `GET /api/v1/archive` response
 * directly — already grouped Year → Month → Photos and ordered newest
 * first; no client-side regrouping or re-sorting. An empty archive is a
 * valid state and renders nothing.
 */
export default function ArchiveSection({
  years,
}: {
  years: ArchiveYearData[];
}) {
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
