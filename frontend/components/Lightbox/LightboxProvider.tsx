"use client";

import {
  createContext,
  useCallback,
  useContext,
  useState,
  type ReactNode,
} from "react";
import Lightbox from "./Lightbox";
import type { LightboxPhoto } from "./types";

/**
 * LightboxProvider — thin Client Component.
 *
 * Holds the single piece of global lightbox state: which photo is open
 * (or null). Triggers call `useLightbox()(photo)` to open; one shared
 * Lightbox overlay is rendered here — not one instance per photo.
 */

type OpenLightbox = (photo: LightboxPhoto) => void;

const LightboxContext = createContext<OpenLightbox>(() => {});

export function useLightbox(): OpenLightbox {
  return useContext(LightboxContext);
}

export default function LightboxProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [photo, setPhoto] = useState<LightboxPhoto | null>(null);

  const open = useCallback((next: LightboxPhoto) => setPhoto(next), []);
  const close = useCallback(() => setPhoto(null), []);

  return (
    <LightboxContext.Provider value={open}>
      {children}
      {photo ? <Lightbox photo={photo} onClose={close} /> : null}
    </LightboxContext.Provider>
  );
}
