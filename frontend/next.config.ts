import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      // Backend-delivered fixed Cloudinary derivatives (production).
      {
        protocol: "https",
        hostname: "res.cloudinary.com",
        pathname: "/**",
      },
      // Memory-profile backend development placeholder media host.
      {
        protocol: "https",
        hostname: "images.example.test",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
