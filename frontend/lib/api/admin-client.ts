/**
 * Browser-side admin API client.
 *
 * Deliberately separate from the server-only public client (lib/api/client.ts)
 * — this module is used by Client Components under /admin.
 *
 * Security model:
 * - every Spring request uses credentials: "include"; the HttpOnly session
 *   cookie is owned entirely by the browser/backend
 * - the CSRF token lives in module memory only — never localStorage,
 *   sessionStorage, IndexedDB, or document.cookie
 * - mutations carry the backend-provided CSRF header name; a single
 *   CSRF_INVALID triggers one token refresh + one retry, never a loop
 * - Cloudinary upload goes browser → Cloudinary directly, without Spring
 *   credentials
 */

import type {
  AdminPhoto,
  AdminPhotoPage,
  AdminSession,
  CreatePhotoInput,
  CsrfToken,
  PhotoMetadataInput,
  UploadSignature,
} from "@/types/admin";

const BASE_URL = process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL;

/* ---------- errors ---------- */

export interface AdminFieldError {
  field: string;
  code: string;
  message: string;
}

export class AdminApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors?: AdminFieldError[];
  readonly retryAfter?: string | null;

  constructor(
    message: string,
    status: number,
    code: string,
    fieldErrors?: AdminFieldError[],
    retryAfter?: string | null,
  ) {
    super(message);
    this.name = "AdminApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.retryAfter = retryAfter;
  }
}

async function parseError(response: Response): Promise<AdminApiError> {
  let code = "UNKNOWN_ERROR";
  let message = `Request failed (${response.status})`;
  let fieldErrors: AdminFieldError[] | undefined;
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object") {
      const record = body as Record<string, unknown>;
      if (typeof record.code === "string") code = record.code;
      if (typeof record.message === "string") message = record.message;
      if (Array.isArray(record.fieldErrors)) {
        fieldErrors = record.fieldErrors as AdminFieldError[];
      }
    }
  } catch {
    // non-JSON error body — keep defaults
  }
  return new AdminApiError(
    message,
    response.status,
    code,
    fieldErrors,
    response.headers.get("Retry-After"),
  );
}

/* ---------- CSRF memory state + session-expiry hook ---------- */

let csrfToken: CsrfToken | null = null;

let unauthorizedHandler: (() => void) | null = null;

/** Registered by AdminShell; invoked on any 401 to return UI to login. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

/** Drops the in-memory CSRF token (logout / session expiry). */
export function clearAdminMemoryState(): void {
  csrfToken = null;
}

/* ---------- core request ---------- */

function baseUrl(): string {
  if (!BASE_URL) {
    throw new AdminApiError(
      "NEXT_PUBLIC_ADMIN_API_BASE_URL is not configured.",
      0,
      "CONFIGURATION_MISSING",
    );
  }
  return BASE_URL.replace(/\/+$/, "");
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  includeCsrf = false,
): Promise<T> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    ...(init.headers as Record<string, string> | undefined),
  };
  if (includeCsrf) {
    if (!csrfToken) {
      throw new AdminApiError("CSRF token missing.", 0, "CSRF_MISSING");
    }
    headers[csrfToken.headerName] = csrfToken.token;
  }

  let response: Response;
  try {
    response = await fetch(`${baseUrl()}${path}`, {
      credentials: "include",
      ...init,
      headers,
    });
  } catch {
    throw new AdminApiError("Admin API is unreachable.", 0, "NETWORK_ERROR");
  }

  if (response.status === 401) {
    unauthorizedHandler?.();
  }
  if (!response.ok) {
    throw await parseError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

async function mutation<T>(
  path: string,
  method: "POST" | "PUT" | "DELETE",
  body?: unknown,
): Promise<T> {
  const execute = () =>
    request<T>(
      path,
      {
        method,
        headers:
          body !== undefined
            ? { "Content-Type": "application/json" }
            : undefined,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      },
      true,
    );

  try {
    return await execute();
  } catch (error) {
    if (
      error instanceof AdminApiError &&
      error.status === 403 &&
      error.code === "CSRF_INVALID"
    ) {
      await refreshCsrf();
      return execute(); // retry exactly once
    }
    throw error;
  }
}

/* ---------- CSRF + session ---------- */

export async function refreshCsrf(): Promise<void> {
  csrfToken = await request<CsrfToken>("/api/v1/admin/csrf");
}

export function getSession(): Promise<AdminSession> {
  return request<AdminSession>("/api/v1/admin/session");
}

export async function login(
  username: string,
  password: string,
): Promise<void> {
  if (!csrfToken) {
    await refreshCsrf();
  }
  await mutation("/api/v1/admin/session", "POST", { username, password });
  // The backend rotates the session id on login (fixation protection), so
  // the pre-login CSRF token is stale — always replace it immediately.
  await refreshCsrf();
}

export async function logout(): Promise<void> {
  try {
    await mutation("/api/v1/admin/session", "DELETE");
  } finally {
    clearAdminMemoryState();
  }
}

/* ---------- admin photos ---------- */

const PAGE_SIZE = 24;

export function listPhotos(page: number): Promise<AdminPhotoPage> {
  return request<AdminPhotoPage>(
    `/api/v1/admin/photos?page=${page}&size=${PAGE_SIZE}`,
  );
}

export function updatePhoto(
  id: string,
  input: PhotoMetadataInput,
): Promise<AdminPhoto> {
  return mutation<AdminPhoto>(
    `/api/v1/admin/photos/${encodeURIComponent(id)}`,
    "PUT",
    input,
  );
}

export function deletePhoto(id: string): Promise<void> {
  return mutation<void>(
    `/api/v1/admin/photos/${encodeURIComponent(id)}`,
    "DELETE",
  );
}

/* ---------- direct upload ---------- */

export interface UploadSignatureRequest {
  fileName: string;
  contentType: string;
  bytes: number;
}

export function requestUploadSignature(
  input: UploadSignatureRequest,
): Promise<UploadSignature> {
  return mutation<UploadSignature>(
    "/api/v1/admin/uploads/signature",
    "POST",
    input,
  );
}

export function createPhoto(input: CreatePhotoInput): Promise<AdminPhoto> {
  return mutation<AdminPhoto>("/api/v1/admin/photos", "POST", input);
}

/**
 * Browser → Cloudinary direct upload via XHR (for real 0–100% progress).
 * Sends exactly the fields signed by Spring — nothing extra, nothing signed
 * in the browser, no Spring credentials.
 */
export function uploadToCloudinary(
  signature: UploadSignature,
  file: File,
  onProgress: (percent: number) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(
      "POST",
      `https://api.cloudinary.com/v1_1/${signature.cloudName}/${signature.resourceType}/upload`,
    );

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(
          Math.min(100, Math.round((event.loaded / event.total) * 100)),
        );
      }
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve();
      } else {
        reject(
          new AdminApiError(
            "Cloudinary upload failed.",
            xhr.status,
            "CLOUDINARY_UPLOAD_FAILED",
          ),
        );
      }
    };
    xhr.onerror = () =>
      reject(
        new AdminApiError(
          "Cloudinary upload failed.",
          0,
          "CLOUDINARY_UPLOAD_FAILED",
        ),
      );

    const form = new FormData();
    form.append("file", file);
    form.append("api_key", signature.apiKey);
    form.append("timestamp", String(signature.timestamp));
    form.append("signature", signature.signature);
    form.append("public_id", signature.publicId);
    form.append("upload_preset", signature.uploadPreset);
    form.append("type", signature.type);
    form.append("overwrite", String(signature.overwrite));
    xhr.send(form);
  });
}
