"use client";

import { useEffect, useRef, type ReactNode } from "react";
import styles from "./PhotoStream.module.css";

/**
 * PhotoStream — thin Client Component (interaction shell).
 *
 * Owns only browser interaction over a scrollable container:
 * - vertical mouse wheel → horizontal scroll (desktop)
 * - mouse pointer drag (touch keeps native swipe scrolling)
 * - very slow idle auto drift (~0.45px/frame), paused by any user input
 * - seamless loop: the track is duplicated once, so when scrollLeft passes
 *   the halfway point it is silently shifted back by one copy
 *
 * Auto drift is disabled under prefers-reduced-motion.
 * Server-rendered children (the photo track) pass through untouched.
 */

const DRIFT_PX_PER_FRAME = 0.45;
const IDLE_RESUME_MS = 2500;

export default function PhotoStream({ children }: { children: ReactNode }) {
  const trackRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = trackRef.current;
    if (!el) return;

    const motionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    let idle = true;
    let idleTimer: number | undefined;

    const pauseDrift = () => {
      idle = false;
      window.clearTimeout(idleTimer);
      idleTimer = window.setTimeout(() => {
        idle = true;
      }, IDLE_RESUME_MS);
    };

    // Seamless reset: content is duplicated, so the midpoint of scrollWidth
    // is visually identical to offset 0.
    const keepInLoop = () => {
      const half = el.scrollWidth / 2;
      if (half > 0 && el.scrollLeft >= half) {
        el.scrollLeft -= half;
      }
    };

    // scrollLeft is integer-quantized by the browser, so the sub-pixel
    // drift speed must be accumulated in JS and only written out.
    let position = el.scrollLeft;

    const onScroll = () => {
      keepInLoop();
      // Re-sync the drift accumulator after external scroll jumps
      // (user input, loop reset) without destroying sub-pixel progress.
      if (Math.abs(el.scrollLeft - position) > 4) {
        position = el.scrollLeft;
      }
    };

    const onWheel = (event: WheelEvent) => {
      if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
        event.preventDefault();
        el.scrollLeft += event.deltaY;
      }
      pauseDrift();
    };

    // Mouse-only drag; touch devices use native horizontal scrolling.
    let dragging = false;
    let dragStartX = 0;
    let dragStartScroll = 0;

    const onPointerDown = (event: PointerEvent) => {
      if (event.pointerType !== "mouse") return;
      dragging = true;
      dragStartX = event.clientX;
      dragStartScroll = el.scrollLeft;
      el.classList.add(styles.dragging);
      pauseDrift();
    };
    const onPointerMove = (event: PointerEvent) => {
      if (!dragging) return;
      el.scrollLeft = dragStartScroll - (event.clientX - dragStartX);
    };
    const onPointerUp = () => {
      dragging = false;
      el.classList.remove(styles.dragging);
    };

    el.addEventListener("wheel", onWheel, { passive: false });
    el.addEventListener("pointerdown", onPointerDown);
    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);
    el.addEventListener("scroll", onScroll, { passive: true });

    let frame = 0;
    const drift = () => {
      if (idle && !dragging && !motionQuery.matches) {
        const half = el.scrollWidth / 2;
        position += DRIFT_PX_PER_FRAME;
        if (half > 0 && position >= half) position -= half;
        el.scrollLeft = position;
      } else {
        // Re-sync after user input or loop reset.
        position = el.scrollLeft;
      }
      frame = requestAnimationFrame(drift);
    };
    frame = requestAnimationFrame(drift);

    return () => {
      cancelAnimationFrame(frame);
      window.clearTimeout(idleTimer);
      el.removeEventListener("wheel", onWheel);
      el.removeEventListener("pointerdown", onPointerDown);
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
      el.removeEventListener("scroll", onScroll);
    };
  }, []);

  return (
    <div ref={trackRef} className={styles.stream}>
      {children}
    </div>
  );
}
