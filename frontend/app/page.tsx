import Hero from "@/components/Hero/Hero";
import PhotoStreamSection from "@/components/PhotoStream/PhotoStreamSection";
import ArchiveSection from "@/components/Archive/ArchiveSection";
import LightboxProvider from "@/components/Lightbox/LightboxProvider";
import { getArchive, getFeaturedPhotos } from "@/lib/api/photos";

/**
 * Homepage — Server Component.
 *
 * All backend reads happen here on the server; the featured pool is fetched
 * once and shared by Hero and PhotoStream. The route renders dynamically
 * per request while the underlying API responses stay cached for 300s via
 * the Data Cache (see lib/api/photos.ts), so the build never depends on a
 * reachable backend.
 */
export const dynamic = "force-dynamic";

export default async function HomePage() {
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
