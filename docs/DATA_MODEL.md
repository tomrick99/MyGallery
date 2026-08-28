# MyGallery — Data Model and API DTOs

**Version:** 0.1  
**Status:** Draft  
**Scope:** V1 Photo domain only

---

## 1. Model Boundary

V1 has one core persisted entity: `PhotoEntity`.

```text
PostgreSQL
└── photos                 # metadata and publication authority

Cloudinary
└── private original       # image bytes; referenced by cloudinary_public_id
```

There is no V1 `User`, `Role`, `Tag`, `Category`, `Archive`, `Month`, `Upload`, or `EXIF` entity. Archive is a read model built from Photo rows; the one administrator is configured through secrets rather than a user table.

JPA entities are internal persistence models. REST controllers only serialize response DTOs.

---

## 2. Photo Entity

### 2.1 Field definitions

| Java field | PostgreSQL column/type | Required | Default | Validation / meaning |
|---|---|---:|---|---|
| `id` | `UUID` | Yes | Application-generated UUID | Stable opaque identifier; primary key |
| `title` | `VARCHAR(200)` | No | `NULL` | Optional title; trimmed, with blank input normalized to null; never generated from the filename |
| `takenAt` | `DATE` | Yes | — | Local calendar date of capture; archive authority |
| `location` | `VARCHAR(200)` | No | `NULL` | Public display text only; blank normalized to null |
| `cloudinaryPublicId` | `VARCHAR(255)` | Yes | — | Unique internal reference to a verified private asset; never in public DTOs |
| `width` | `INTEGER` | Yes | — | Verified original pixel width, `> 0`; not trusted from browser |
| `height` | `INTEGER` | Yes | — | Verified original pixel height, `> 0`; not trusted from browser |
| `featured` | `BOOLEAN` | Yes | `FALSE` | Eligibility for Hero / Selected Frames; does not alter archive order |
| `visibility` | `VARCHAR(16)` | Yes | `PRIVATE` | `PUBLIC` or `PRIVATE`; string enum with DB check constraint |
| `camera` | `VARCHAR(150)` | No | `NULL` | Approved public-facing camera model; no raw EXIF blob |
| `lens` | `VARCHAR(200)` | No | `NULL` | Approved public-facing lens name |
| `focalLength` | `NUMERIC(8,2)` | No | `NULL` | Millimetres, `> 0`; Java `BigDecimal` |
| `aperture` | `NUMERIC(5,2)` | No | `NULL` | f-number, `> 0`; Java `BigDecimal` |
| `shutterSpeed` | `NUMERIC(16,9)` | No | `NULL` | Exposure seconds, `> 0`; supports values such as `0.004000000` |
| `iso` | `INTEGER` | No | `NULL` | Positive ISO value |
| `description` | `TEXT` | No | `NULL` | Trimmed public description; application max 5,000 characters |
| `createdAt` | `TIMESTAMPTZ` | Yes | Current instant | Server-controlled creation audit time |
| `updatedAt` | `TIMESTAMPTZ` | Yes | Current instant | Server-controlled last metadata update time |

### 2.2 Why `takenAt` is `DATE`

The product and frontend contract display `YYYY-MM-DD` and organize works by the photographer's calendar year/month. A timezone-aware instant can move a late-night photograph into a different day when converted, while EXIF commonly contains a local time without a trustworthy timezone. Therefore V1 stores a date, not an instant.

If exact capture time becomes a real product requirement, add a separate migration with an explicit local timestamp and optional timezone/offset semantics. Do not silently reinterpret existing dates.

### 2.3 Numeric EXIF storage

Focal length, aperture, and shutter speed are stored as numeric values with documented units rather than presentation strings. This makes validation and future formatting reliable:

```text
focal_length = 35.00       → public display "35 mm"
aperture = 2.80            → public display "f/2.8"
shutter_speed = 0.004      → public display "1/250 s"
```

The public response may expose formatted strings to match the frontend display model, while admin requests use numeric values with explicit unit names.

---

## 3. Schema Migrations

The deployed initial schema remains owned by the immutable `V1__create_photos.sql` migration:

```sql
CREATE TABLE photos (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    taken_at DATE NOT NULL,
    location VARCHAR(200),
    cloudinary_public_id VARCHAR(255) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    camera VARCHAR(150),
    lens VARCHAR(200),
    focal_length NUMERIC(8, 2),
    aperture NUMERIC(5, 2),
    shutter_speed NUMERIC(16, 9),
    iso INTEGER,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_photos_cloudinary_public_id UNIQUE (cloudinary_public_id),
    CONSTRAINT ck_photos_title_nonblank CHECK (btrim(title) <> ''),
    CONSTRAINT ck_photos_cloudinary_id_nonblank CHECK (btrim(cloudinary_public_id) <> ''),
    CONSTRAINT ck_photos_dimensions CHECK (width > 0 AND height > 0),
    CONSTRAINT ck_photos_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_photos_focal_length CHECK (focal_length IS NULL OR focal_length > 0),
    CONSTRAINT ck_photos_aperture CHECK (aperture IS NULL OR aperture > 0),
    CONSTRAINT ck_photos_shutter_speed CHECK (shutter_speed IS NULL OR shutter_speed > 0),
    CONSTRAINT ck_photos_iso CHECK (iso IS NULL OR iso > 0)
);

CREATE INDEX idx_photos_public_taken_at
    ON photos (taken_at DESC, created_at DESC, id DESC)
    WHERE visibility = 'PUBLIC';

CREATE INDEX idx_photos_public_featured_taken_at
    ON photos (taken_at DESC, created_at DESC, id DESC)
    WHERE visibility = 'PUBLIC' AND featured = TRUE;
```

`V2__allow_null_photo_titles.sql` evolves production databases without rewriting existing rows:

```sql
ALTER TABLE photos
    ALTER COLUMN title DROP NOT NULL;

ALTER TABLE photos
    DROP CONSTRAINT ck_photos_title_nonblank;

ALTER TABLE photos
    ADD CONSTRAINT ck_photos_title_nonblank
        CHECK (title IS NULL OR btrim(title) <> '');
```

Application validation remains stricter where needed (length, `@PastOrPresent`, upload limit). Database constraints provide a final integrity boundary and protect writes outside normal controllers.

`updated_at` is set on every successful application update. A database trigger is unnecessary in V1 because the Spring application is the only writer; migration scripts must set it explicitly when performing data updates.

---

## 4. Required and Optional Rules

### 4.1 Required because the product cannot work safely without them

- `takenAt`: determines archive year/month and visible date
- `cloudinaryPublicId`: links metadata to the verified asset
- `width` and `height`: reserve layout space and derive orientation/aspect ratio
- `featured`: explicit boolean; null would make selection ambiguous
- `visibility`: explicit publication state, defaulting to `PRIVATE`
- audit timestamps: operational traceability

### 4.2 Optional because the photograph can still be published without them

- title
- location
- camera and lens
- focal length, aperture, shutter speed, ISO
- description

Optional text is normalized: trim whitespace, convert an empty string to `NULL`, and enforce maximum length after trimming.

### 4.3 Safe creation invariant

A Photo row is created only after the backend verifies a completed Cloudinary asset. There is no persisted half-uploaded/draft Photo state in V1. A newly created row defaults to `PRIVATE` unless an authenticated create request explicitly asks to publish and passes all validation.

---

## 5. Derived Fields

The following values are not columns:

| Derived field | Source | Rule |
|---|---|---|
| `year` | `takenAt` | `takenAt.getYear()` |
| `month` | `takenAt` | Numeric `1–12` |
| `orientation` | `width`, `height` | `landscape` if width > height; `portrait` if width < height; otherwise `square` |
| `aspectRatio` | `width`, `height` | Decimal `width / height`, rounded only for JSON presentation |

They are computed while mapping to response DTOs or in an archive projection. Persisting them would allow contradictions such as `takenAt=2026-08-14` with `year=2025` or `width > height` with `orientation=portrait`.

Archive queries may use PostgreSQL `EXTRACT` in the projection/grouping result, but not as stored columns. Year/month request filters are translated to half-open date ranges:

```text
year=2026           → [2026-01-01, 2027-01-01)
year=2026&month=8   → [2026-08-01, 2026-09-01)
```

---

## 6. Visibility

### 6.1 V1 states

| State | Public list | Featured | Public detail | Admin read/write |
|---|---:|---:|---:|---:|
| `PUBLIC` | Yes | Only if `featured=true` | Yes | Yes |
| `PRIVATE` | No | No, even if `featured=true` | Returns 404 | Yes |

`featured=true` never overrides visibility. Keeping a PRIVATE photo featured is allowed so it can become eligible automatically when deliberately published later.

Public repository/query methods encode `visibility = PUBLIC` directly. Controllers do not fetch all rows and filter in memory. A private UUID and an unknown UUID produce the same public 404 to avoid leaking private inventory.

### 6.2 Future `UNLISTED`

`UNLISTED` is not included in the V1 constraint because it requires a precise discovery/share-token policy. If needed later, add it through a Flyway migration and define whether detail access requires an opaque share token. It must never be treated as PUBLIC merely because someone knows the sequential route; UUID alone is not authorization.

---

## 7. Invariants and Update Rules

1. `cloudinaryPublicId`, `width`, and `height` come from server-side Cloudinary verification.
2. Public IDs are unique and generated by the backend; `overwrite=false` keeps image URLs immutable.
3. `id`, `createdAt`, and Cloudinary identity are not mutable through `PUT`.
4. `updatedAt` changes only after a successful write.
5. Setting visibility to PUBLIC does not require title/EXIF/location/description, but still requires a capture date and a valid asset.
6. Raw URLs, transformations, GPS, serial numbers, thumbnails embedded in EXIF, and arbitrary metadata maps are never accepted as Photo fields.
7. A create/update request is rejected as a whole if any supplied public metadata fails validation; no partial mutation.
8. Public ordering is deterministic: `takenAt`, then `createdAt`, then `id`, all in the requested direction.

---

## 8. Index Strategy

| Index | Query supported | Reason |
|---|---|---|
| Primary key on `id` | Detail/admin lookup | Default UUID lookup |
| Unique `cloudinary_public_id` | Upload completion and deduplication | One asset belongs to at most one Photo |
| Partial public date index | Public list/archive/date range | Excludes private rows and matches newest-first order |
| Partial featured public date index | Hero/selected frame pool | Makes the frequent small featured query direct |

No index is added for every optional field. V1 has no public search/category filtering, and a standalone low-cardinality `visibility` index would add write cost without improving the main queries. Indexes should be justified with production query plans as the collection grows.

Archive has no table or materialized view in V1. It is a deterministic grouped read over the public date index.

---

## 9. DTO Architecture

### 9.1 `PhotoEntity`

Internal JPA mapping of the `photos` row. It includes the Cloudinary public ID and visibility and must not carry JSON serialization annotations as a substitute for a DTO boundary.

### 9.2 `PhotoSummaryResponse`

Used for archive, stream, Hero pool, and lightbox navigation:

```json
{
  "id": "735ed9b0-78f7-4ad6-afc6-eaf47b5a92d7",
  "title": "Orange Steel Over Water",
  "takenAt": "2026-08-14",
  "year": 2026,
  "month": 8,
  "location": "Coastal Pier",
  "orientation": "landscape",
  "aspectRatio": 1.5,
  "featured": true,
  "image": {
    "thumbnailUrl": "https://res.cloudinary.com/.../t_gallery_thumbnail/...",
    "cardUrl": "https://res.cloudinary.com/.../t_gallery_card/...",
    "displayUrl": "https://res.cloudinary.com/.../t_gallery_display/..."
  }
}
```

Notes:

- `image` contains only fixed, eager/allowlisted public derivative URLs; a delivery signature may be present but is never treated as a secret
- `width` and `height` need not be exposed because `aspectRatio` is enough for layout; they may be added to the image DTO if `next/image` integration benefits from exact dimensions
- `visibility` and `cloudinaryPublicId` are deliberately absent
- `title` is nullable and is serialized as JSON `null` when a photo is intentionally untitled
- Derived `year`, `month`, `orientation`, and `aspectRatio` are response conveniences, not persisted data

This is the formal backend shape. During Step 4 integration, the frontend `types/photo.ts` should model `image` (or its API adapter should map `cardUrl` to its current `imageUrl`) without changing the visual architecture.

### 9.3 `PhotoResponse`

Extends the summary shape with public detail fields:

```json
{
  "id": "735ed9b0-78f7-4ad6-afc6-eaf47b5a92d7",
  "title": "Orange Steel Over Water",
  "takenAt": "2026-08-14",
  "year": 2026,
  "month": 8,
  "location": "Coastal Pier",
  "orientation": "landscape",
  "aspectRatio": 1.5,
  "featured": true,
  "image": {
    "thumbnailUrl": "https://res.cloudinary.com/.../t_gallery_thumbnail/...",
    "cardUrl": "https://res.cloudinary.com/.../t_gallery_card/...",
    "displayUrl": "https://res.cloudinary.com/.../t_gallery_display/..."
  },
  "description": "Late light across the pier.",
  "camera": "Example Camera",
  "lens": "Example 35mm Lens",
  "focalLength": "35 mm",
  "aperture": "f/2.8",
  "shutterSpeed": "1/250 s",
  "iso": 400
}
```

Every optional field is present with `null` when unknown. No raw EXIF object is returned.

### 9.4 `PhotoCreateRequest`

Sent only after direct upload:

```json
{
  "cloudinaryPublicId": "mygallery/originals/generated-opaque-id",
  "title": "Orange Steel Over Water",
  "takenAt": "2026-08-14",
  "location": "Coastal Pier",
  "featured": false,
  "visibility": "PRIVATE",
  "camera": null,
  "lens": null,
  "focalLengthMm": null,
  "aperture": null,
  "shutterSpeedSeconds": null,
  "iso": null,
  "description": null
}
```

The request does not contain authoritative `width`, `height`, URL, format, bytes, derived fields, IDs, or timestamps. The backend retrieves technical asset values from Cloudinary.

`title` is optional. Non-blank titles are trimmed; blank or whitespace-only values normalize to `null`. The backend does not generate `Untitled` or derive a title from the uploaded filename.

### 9.5 `PhotoUpdateRequest`

Contains the same mutable metadata as create except `cloudinaryPublicId`. `takenAt`, `featured`, and `visibility` are required in every PUT; optional fields, including `title`, may be explicitly `null` to clear them.

PUT does not replace the image. Asset replacement, if ever needed, should be a deliberate operation with verification and cleanup semantics rather than allowing a generic metadata update to swap public IDs.

### 9.6 Admin response

An `AdminPhotoResponse` may include a nullable title, visibility, verified dimensions, update timestamps, and the internal public ID needed for operational management. It is never reused as a public DTO and every endpoint returning it requires the admin session.

### 9.7 Page response

`PhotoPageResponse` contains:

```text
items: PhotoSummaryResponse[]
page: integer, zero-based
size: integer
totalElements: long
totalPages: integer
```

Page size defaults to 24 and is capped at 100. Negative pages, out-of-range sizes, invalid months, or `month` without `year` return `400 INVALID_FILTER`.

---

## 10. Archive Response Structure

`ArchiveResponse` is a read DTO, not an entity:

```json
{
  "years": [
    {
      "year": 2026,
      "photoCount": 3,
      "months": [
        {
          "month": 8,
          "label": "AUGUST",
          "photoCount": 2,
          "photos": [
            { "id": "...", "title": null, "takenAt": "2026-08-14", "image": {} }
          ]
        },
        {
          "month": 7,
          "label": "JULY",
          "photoCount": 1,
          "photos": []
        }
      ]
    }
  ]
}
```

Rules:

- empty years and empty months are omitted
- years sort descending
- months sort `12 → 1`
- photos sort newest first with deterministic tie-breakers
- counts include only PUBLIC photos
- `label` uses stable uppercase English month names to match the current editorial design; the numeric month remains the semantic value
- optional `year` query filtering returns zero or one year entry without changing the shape

No nested pagination is added in V1. If archive payload size later becomes material, the existing `year` filter supports loading one year at a time without redesigning Photo.

---

## 11. Future EXIF Extension

V1 intentionally stores only this allowlist:

- camera
- lens
- focal length
- aperture
- shutter speed
- ISO
- capture date (through `takenAt`)

The upload pipeline may read candidate values, but the administrator reviews them before persistence/publication. Never persist or return an unbounded raw EXIF JSON blob.

Explicitly excluded unless a later product decision adds a safe field:

- GPS latitude/longitude and altitude
- device/body/lens serial numbers
- owner/artist/copyright fields copied blindly from a device
- embedded thumbnails
- comments, filesystem paths, editing history, and vendor maker notes

Public Cloudinary derivatives strip metadata. The private original may retain its embedded metadata inside Cloudinary, but it is never delivered to a public visitor.

Future tags/categories, if approved by product requirements, require their own model and migration. They are not represented as a placeholder JSON column in V1.

---

## 12. Flyway Evolution Rules

1. `V1__create_photos.sql` creates only the Photo schema and indexes.
2. A deployed migration is immutable; corrections use `V2`, `V3`, and so on.
3. Enum expansion such as `UNLISTED` requires a migration that updates the check constraint before application code emits the new value.
4. New required columns use a safe expand/backfill/constrain sequence when rows already exist.
5. Destructive type changes and column drops require a verified backup and rollback plan.
6. Production uses Flyway plus JPA schema validation; Hibernate never creates or updates the production schema.
