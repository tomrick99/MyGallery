import { mockFeaturedPhotos } from "@/lib/mock/photos";
import HeroPicker from "./HeroPicker";
import styles from "./Hero.module.css";

/**
 * Hero — Server Component.
 *
 * Fetches the featured pool (currently local mock data; later
 * `getFeaturedPhotos()` from lib/api) and renders the editorial structure.
 * The photograph itself is rendered by the thin HeroPicker client boundary,
 * which keeps server HTML deterministic (first pool entry as fallback) and
 * randomizes only after hydration — client-side presentation randomness.
 */
export default function Hero() {
  const featuredPhotos = mockFeaturedPhotos;

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
