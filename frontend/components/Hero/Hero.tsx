import type { PhotoSummary } from "@/types/photo";
import HeroPicker from "./HeroPicker";
import styles from "./Hero.module.css";

/**
 * Hero — Server Component.
 *
 * Receives the featured pool fetched by the homepage server component and
 * renders the editorial structure. The photograph itself is rendered by the
 * thin HeroPicker client boundary, which keeps server HTML deterministic
 * (first pool entry as fallback) and randomizes only after hydration —
 * client-side presentation randomness. An empty pool renders the text-only
 * editorial state: no crash, no fake photograph.
 */
export default function Hero({
  featuredPhotos,
}: {
  featuredPhotos: PhotoSummary[];
}) {
  return (
    <section className={styles.hero} aria-label="Featured photography">
      <div className={styles.text}>
        <h1 className={styles.title}>
          <span className={styles.line}>TOM</span>
          <span className={`${styles.line} ${styles.lineIndent}`}>RICK</span>
        </h1>
        <p className={styles.sub}>
          <span>PHOTOGRAPHY</span>
          <span className={styles.years}>2024 — 2026</span>
        </p>
        <p className={styles.note}>
          A personal photographic archive.
          <br />
          Selected frames, arranged by time.
        </p>
      </div>
      {featuredPhotos.length > 0 ? <HeroPicker photos={featuredPhotos} /> : null}
    </section>
  );
}
