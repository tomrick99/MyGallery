import Hero from "@/components/Hero/Hero";
import PhotoStreamSection from "@/components/PhotoStream/PhotoStreamSection";
import ArchiveSection from "@/components/Archive/ArchiveSection";

export default function HomePage() {
  return (
    <main>
      <Hero />
      <PhotoStreamSection />
      <ArchiveSection />
    </main>
  );
}
