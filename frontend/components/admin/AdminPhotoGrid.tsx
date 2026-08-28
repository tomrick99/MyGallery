"use client";

import Image from "next/image";
import type { AdminPhoto } from "@/types/admin";
import styles from "./admin.module.css";

/**
 * AdminPhotoGrid — photography-first cards with restrained status labels.
 * Uses backend derivative URLs as provided (thumbnail, unoptimized).
 */
export default function AdminPhotoGrid({
  photos,
  onEdit,
}: {
  photos: AdminPhoto[];
  onEdit: (photo: AdminPhoto) => void;
}) {
  return (
    <div className={styles.grid} role="list">
      {photos.map((photo) => (
        <button
          key={photo.id}
          type="button"
          role="listitem"
          className={styles.card}
          onClick={() => onEdit(photo)}
          aria-label={`Edit ${photo.title}`}
        >
          <span className={styles.cardFrame}>
            <Image
              src={photo.image.thumbnailUrl}
              alt={photo.title}
              width={480}
              height={Math.round(480 / photo.aspectRatio)}
              unoptimized
              className={styles.cardImage}
            />
          </span>
          <span className={styles.cardMeta}>
            <span className={styles.cardTitle}>{photo.title}</span>
            <span className={styles.cardDate}>{photo.takenAt}</span>
            <span className={styles.cardStatusRow}>
              <span
                className={
                  photo.visibility === "PUBLIC"
                    ? styles.statusPublic
                    : styles.statusPrivate
                }
              >
                {photo.visibility}
              </span>
              {photo.featured ? (
                <span className={styles.statusFeatured}>FEATURED</span>
              ) : null}
            </span>
          </span>
        </button>
      ))}
    </div>
  );
}
