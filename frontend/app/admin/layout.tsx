import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "Admin — MyGallery",
  robots: { index: false, follow: false },
};

/**
 * /admin — private operational UI. Server shell only; all backend traffic
 * happens in the browser-side admin client at request time, never at build.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return children;
}
