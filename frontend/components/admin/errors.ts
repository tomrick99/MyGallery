import { AdminApiError } from "@/lib/api/admin-client";

/** Maps sanitized backend error codes onto calm admin-facing messages. */
export function describeAdminError(error: unknown): string {
  if (!(error instanceof AdminApiError)) return "Something went wrong.";

  switch (error.code) {
    case "PHOTO_ASSET_ALREADY_LINKED":
      return "This uploaded asset is already linked to a photo.";
    case "INVALID_UPLOADED_ASSET":
      return "The uploaded asset failed backend verification.";
    case "RATE_LIMITED":
      return `Too many requests.${error.retryAfter ? ` Try again in ${error.retryAfter}s.` : ""}`;
    case "MEDIA_PROVIDER_UNAVAILABLE":
      return "The media provider is temporarily unavailable. Your upload is safe — retry saving.";
    case "UPLOAD_TOO_LARGE":
      return "The file exceeds the upload limit.";
    case "NETWORK_ERROR":
      return "The admin API is unreachable.";
    case "CLOUDINARY_UPLOAD_FAILED":
      return "The direct upload to the media provider failed.";
    default:
      return error.message;
  }
}
