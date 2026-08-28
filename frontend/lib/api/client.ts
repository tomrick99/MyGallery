/**
 * MyGallery — API client foundation.
 *
 * Server Components are the primary API consumers
 * (docs/FRONTEND_TECHNICAL_DESIGN.md §7), so the base URL is read from the
 * non-public `API_BASE_URL` env variable: it never enters browser bundles.
 * No secret is involved — admin/session requests do not exist in V1 frontend.
 *
 * The `server-only` import is a hard boundary: any accidental import of this
 * module from a Client Component fails the build immediately.
 */

import "server-only";

const API_BASE_URL = process.env.API_BASE_URL;

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * Minimal typed fetch wrapper for the Spring Boot REST API (`/api/v1/*`).
 * Throws `ApiError` for missing configuration and non-2xx responses.
 */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  if (!API_BASE_URL) {
    throw new ApiError("API_BASE_URL is not configured.", 0);
  }

  // Robust join: tolerate a trailing slash in API_BASE_URL and never emit
  // double slashes such as http://host//api/v1/archive.
  const baseUrl = API_BASE_URL.replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  const response = await fetch(`${baseUrl}${normalizedPath}`, {
    headers: { Accept: "application/json", ...init?.headers },
    ...init,
  });

  if (!response.ok) {
    throw new ApiError(
      `API request failed: ${response.status} ${response.statusText} (${path})`,
      response.status,
    );
  }

  return response.json() as Promise<T>;
}
