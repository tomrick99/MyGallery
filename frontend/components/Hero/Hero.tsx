import Image from "next/image";
import type { PhotoSummary } from "@/types/photo";
import { photoAltText, photoCaptionText } from "@/lib/photos/display";
import styles from "./Hero.module.css";

/**
 * Hero — Server Component.
 *
 * The homepage is request-time rendered (see app/page.tsx `connection()`),
 * so the random featured photo is selected here on the server, once per
 * request. The chosen photograph is the first and only Hero image sent to
 * the browser — no post-hydration swap, no hydration mismatch, no double
 * download. An empty pool renders the text-only editorial state.
 */
export default function Hero({
  featuredPhotos,
}: {
  featuredPhotos: PhotoSummary[];
}) {
  // Intentional impurity: this Server Component renders once per request
  // (app/page.tsx awaits connection()), so Math.random() here yields one
  // random featured photo per fresh request — not a client re-render bug.
  const heroPhoto =
    featuredPhotos.length > 0
      ? // eslint-disable-next-line react-hooks/purity
        featuredPhotos[Math.floor(Math.random() * featuredPhotos.length)]
      : null;

  return (
    <section className={styles.hero} aria-label="Featured photography">
      <div className={styles.text}>
        <h1 className={styles.title}>
          <span className={styles.line}>FRAMES</span>
          <span className={`${styles.line} ${styles.lineIndent}`}>BY TOM</span>
        </h1>
        <p className={styles.sub}>
          <span>PERSONAL PHOTOGRAPHIC ARCHIVE</span>
          <span className={styles.years}>2023 — ∞</span>
        </p>
        <p className={styles.note}>
          A personal photographic archive.
          <br />
          Selected frames, arranged by time.
        </p>
      </div>
      {heroPhoto ? (
        <figure className={styles.figure}>
          <div className={styles.frame}>
            <Image
              src={heroPhoto.image.displayUrl}
              alt={photoAltText(heroPhoto)}
              fill
              priority
              unoptimized
              sizes="(max-width: 860px) 100vw, 42vw"
              className={styles.image}
            />
          </div>
          <figcaption className={styles.caption}>
            {photoCaptionText(heroPhoto)} · {heroPhoto.year}
          </figcaption>
        </figure>
      ) : null}
    </section>
  );
}
