import Image from "next/image";
import type { PhotoSummary } from "@/types/photo";
import styles from "./PhotoStream.module.css";

/**
 * PhotoStreamTrack — Server Component.
 *
 * Renders the photo figures. The list is duplicated once (A B C D A B C D)
 * so the client shell can loop seamlessly; the duplicate copy is
 * aria-hidden with empty alts. Size variants follow a small stable pattern
 * keyed by orientation + index — no nth-child hacks.
 */

const LANDSCAPE_VARIANTS = ["wide", "medium"] as const;

type Variant = "wide" | "medium" | "portrait";

function variantFor(photo: PhotoSummary, index: number): Variant {
  if (photo.orientation === "portrait") return "portrait";
  return LANDSCAPE_VARIANTS[index % LANDSCAPE_VARIANTS.length];
}

export default function PhotoStreamTrack({
  photos,
}: {
  photos: PhotoSummary[];
}) {
  const items = [...photos, ...photos];

  return (
    <>
      {items.map((photo, i) => {
        const duplicate = i >= photos.length;
        const variant = variantFor(photo, i % photos.length);
        const alt = photo.location
          ? `${photo.title} — ${photo.location}`
          : photo.title;
        const caption = photo.location
          ? `${photo.title} — ${photo.location}`
          : photo.title;

        return (
          <figure
            key={`${photo.id}-${i}`}
            className={`${styles.item} ${styles[variant]}`}
            aria-hidden={duplicate || undefined}
          >
            <div className={styles.frame}>
              <Image
                src={photo.image.cardUrl}
                alt={duplicate ? "" : alt}
                fill
                sizes="(max-width: 860px) 78vw, 32vw"
                loading={i < 3 ? "eager" : "lazy"}
                className={styles.image}
              />
            </div>
            <figcaption className={styles.caption}>{caption}</figcaption>
          </figure>
        );
      })}
    </>
  );
}
