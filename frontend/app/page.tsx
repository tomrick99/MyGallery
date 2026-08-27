import Hero from "@/components/Hero/Hero";
import PhotoStreamSection from "@/components/PhotoStream/PhotoStreamSection";
import ArchiveSection from "@/components/Archive/ArchiveSection";
import LightboxProvider from "@/components/Lightbox/LightboxProvider";

export default function HomePage() {
  return (
    <main>
      <Hero />
      <PhotoStreamSection />
      <LightboxProvider>
        <ArchiveSection />
      </LightboxProvider>
    </main>
  );
}
