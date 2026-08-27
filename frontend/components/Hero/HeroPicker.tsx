"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import type { PhotoSummary } from "@/types/photo";
import styles from "./Hero.module.css";

/**
 * HeroPicker — thin Client Component.
 *
 * Receives the serializable featured pool. Initial state is always
 * `photos[0]` — identical to the server-rendered HTML, so hydration never
 * mismatches. After mount, one photo is picked at random; if it differs
 * from the fallback, the swap runs through a restrained opacity fade
 * (disabled under prefers-reduced-motion).
 *
 * It does not fetch, route, or hold any global state.
 */
export default function HeroPicker({ photos }: { photos: PhotoSummary[] }) {
  const [photo, setPhoto] = useState<PhotoSummary>(photos[0]);

  useEffect(() => {
    if (photos.length < 2) return;
    const pick = photos[Math.floor(Math.random() * photos.length)];
    if (pick.id === photos[0].id) return;
    // Defer the one-time random selection to after hydration paint,
    // avoiding a synchronous setState inside the effect body.
    const frame = requestAnimationFrame(() => setPhoto(pick));
    return () => cancelAnimationFrame(frame);
  }, [photos]);

  const alt = photo.location
    ? `${photo.title} — ${photo.location}`
    : photo.title;
  const caption = photo.location
    ? `${photo.title} — ${photo.location} · ${photo.year}`
    : `${photo.title} · ${photo.year}`;

  return (
    <figure className={styles.figure}>
      <div className={styles.frame}>
        <Image
          key={photo.id}
          src={photo.image.displayUrl}
          alt={alt}
          fill
          priority
          sizes="(max-width: 860px) 100vw, 42vw"
          className={styles.image}
        />
      </div>
      <figcaption key={photo.id} className={styles.caption}>
        {caption}
      </figcaption>
    </figure>
  );
}
