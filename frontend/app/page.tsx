import { connection } from "next/server";
import Hero from "@/components/Hero/Hero";
import PhotoStreamSection from "@/components/PhotoStream/PhotoStreamSection";
import ArchiveSection from "@/components/Archive/ArchiveSection";
import LightboxProvider from "@/components/Lightbox/LightboxProvider";
import { getArchive, getFeaturedPhotos } from "@/lib/api/photos";

/**
 * Homepage — Server Component.
 *
 * All backend reads happen here on the server; the featured pool is fetched
 * once and shared by Hero and PhotoStream. `connection()` defers rendering
 * to request time (the build never depends on a reachable backend) while —
 * unlike force-dynamic — preserving the explicit 300s Data Cache policy in
 * lib/api/photos.ts.
 */
export default async function HomePage() {
  await connection();

  const [featuredPhotos, archiveYears] = await Promise.all([
    getFeaturedPhotos(),
    getArchive(),
  ]);

  return (
    <main>
      <Hero featuredPhotos={featuredPhotos} />
      <PhotoStreamSection photos={featuredPhotos} />
      <LightboxProvider>
        <ArchiveSection years={archiveYears} />
      </LightboxProvider>
    </main>
  );
}
