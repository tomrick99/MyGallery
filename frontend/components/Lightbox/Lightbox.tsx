"use client";

import Image from "next/image";
import { useEffect, useRef } from "react";
import type { LightboxPhoto } from "./types";
import styles from "./Lightbox.module.css";

/**
 * Lightbox — fullscreen quiet viewer.
 *
 * - displayUrl, object-fit: contain semantics — the whole photograph stays
 *   visible, portrait stays portrait, landscape stays landscape
 * - close: CLOSE button / Escape / backdrop click (photo click never closes)
 * - body scroll locked while open, restored on close
 * - focus moves into the dialog on open and returns to the trigger on close
 */

const INTRINSIC_WIDTH = 2048;

export default function Lightbox({
  photo,
  onClose,
}: {
  photo: LightboxPhoto;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previouslyFocused =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    dialogRef.current?.focus();

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
        return;
      }
      // Minimal focus containment: keep Tab cycling inside the dialog.
      if (event.key === "Tab" && dialogRef.current) {
        const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
          "button, [tabindex]",
        );
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };

    document.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", onKeyDown);
      previouslyFocused?.focus();
    };
  }, [onClose]);

  const alt = photo.location
    ? `${photo.title} — ${photo.location}`
    : photo.title;

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={photo.title}
        tabIndex={-1}
        className={styles.dialog}
        onClick={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          className={styles.close}
          onClick={onClose}
          aria-label="Close photograph viewer"
        >
          CLOSE
        </button>
        <figure className={styles.figure}>
          <Image
            src={photo.displayUrl}
            alt={alt}
            width={INTRINSIC_WIDTH}
            height={Math.round(INTRINSIC_WIDTH / photo.aspectRatio)}
            className={styles.image}
            priority
          />
          <figcaption className={styles.caption}>
            <span>{photo.title}</span>
            {photo.location ? <span>{photo.location}</span> : null}
            <span>{photo.takenAt}</span>
          </figcaption>
        </figure>
      </div>
    </div>
  );
}
