"use client";

import { useEffect, useRef, type ReactNode } from "react";
import styles from "./admin.module.css";

/**
 * Shared admin modal: dialog semantics, Escape-to-close (unless busy),
 * backdrop click to close, focus moved inside on open and returned on
 * unmount.
 */
export default function AdminModal({
  label,
  busy = false,
  onClose,
  children,
  wide = false,
}: {
  label: string;
  busy?: boolean;
  onClose: () => void;
  children: ReactNode;
  wide?: boolean;
}) {
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previouslyFocused =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    dialogRef.current?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) onClose();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      previouslyFocused?.focus();
    };
  }, [onClose, busy]);

  return (
    <div
      className={styles.modalBackdrop}
      onClick={() => {
        if (!busy) onClose();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={label}
        tabIndex={-1}
        className={`${styles.modal} ${wide ? styles.modalWide : ""}`}
        onClick={(event) => event.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
}
