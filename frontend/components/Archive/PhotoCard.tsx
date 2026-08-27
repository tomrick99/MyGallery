import Image from "next/image";
import type { PhotoSummary } from "@/types/photo";
import type { ArchiveVariant } from "@/lib/photos/layoutVariant";
import PhotoLightboxTrigger from "@/components/Lightbox/PhotoLightboxTrigger";
import styles from "./Archive.module.css";

/**
 * PhotoCard — Server Component (Archive).
 *
 * The Archive is the primary exhibition space: the original photographic
 * composition is always preserved. Intrinsic dimensions are derived from
 * the backend-provided `aspectRatio`, and CSS renders the image at
 * width: 100% / height: auto — no fixed-ratio container, no cover crop.
 * Layout variants only change width and offset.
 */

const INTRINSIC_WIDTH = 1200;

export default function PhotoCard({
  photo,
  variant,
}: {
  photo: PhotoSummary;
  variant: ArchiveVariant;
}) {
  const alt = photo.location
    ? `${photo.title} — ${photo.location}`
    : photo.title;

  return (
    <figure className={`${styles.card} ${styles[variant]}`}>
      <div className={styles.frame}>
        <Image
          src={photo.image.cardUrl}
          alt={alt}
          width={INTRINSIC_WIDTH}
          height={Math.round(INTRINSIC_WIDTH / photo.aspectRatio)}
          sizes="(max-width: 860px) 100vw, 52vw"
          loading="lazy"
          className={styles.image}
        />
        <PhotoLightboxTrigger
          photo={{
            id: photo.id,
            title: photo.title,
            location: photo.location,
            takenAt: photo.takenAt,
            aspectRatio: photo.aspectRatio,
            displayUrl: photo.image.displayUrl,
          }}
        />
      </div>
      <figcaption className={styles.caption}>
        <span>{photo.title}</span>
        {photo.location ? <span>{photo.location}</span> : null}
      </figcaption>
    </figure>
  );
}
