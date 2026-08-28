"use client";

import { useLightbox } from "./LightboxProvider";
import { photoCaptionText } from "@/lib/photos/display";
import type { LightboxPhoto } from "./types";
import styles from "./Lightbox.module.css";

/**
 * PhotoLightboxTrigger — thin Client leaf.
 *
 * A transparent, keyboard-accessible button layered over a Server-rendered
 * PhotoCard frame. It only forwards the minimal serializable payload to
 * the provider; no data logic, no image rendering.
 */
export default function PhotoLightboxTrigger({
  photo,
}: {
  photo: LightboxPhoto;
}) {
  const open = useLightbox();

  return (
    <button
      type="button"
      className={styles.trigger}
      onClick={() => open(photo)}
      aria-label={`View photograph: ${photoCaptionText(photo)}`}
    />
  );
}
