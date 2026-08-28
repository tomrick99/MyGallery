# MyGallery — Backend / Data / Media Technical Design

**Version:** 0.1  
**Status:** Draft  
**Step:** 3B — Backend / Data / Media / Security Technical Design

---

## 1. Scope and Design Principles

本文定义 MyGallery V1 后端、数据、媒体与部署架构。前端实现以
`docs/FRONTEND_TECHNICAL_DESIGN.md` 为准；本文只定义前后端之间的正式契约，不修改前端方案。

核心原则：

- Spring Boot 是全部业务数据与可见性规则的 authority
- 前端只通过 `/api/v1/*` REST API 读取数据，不直连 PostgreSQL
- PostgreSQL 只保存作品 metadata，不保存图片二进制
- Cloudinary 保存原始图片并交付固定规格的衍生图
- 原图默认私有；公开 API 不返回原图 URL 或 `cloudinaryPublicId`
- 后端采用模块化单体，保持部署简单、模块边界清晰
- V1 只建立 Photo 核心领域，不提前建立用户、标签、分类等实体

非目标：Microservices、Redis、Kafka、Kubernetes、GraphQL、Elasticsearch、CMS、复杂 OAuth。

---

## 2. Architecture Overview

```text
                              HTTPS
┌─────────────┐       ┌──────────────────┐       ┌──────────────────────┐
│   Visitor   │ ────► │ Next.js / Vercel│ ────► │ Spring Boot / Railway│
└─────────────┘       └──────────────────┘ REST  └──────────┬───────────┘
                         www.example.com       api.example.com│
                                                            │
                                      ┌─────────────────────┴────────────────────┐
                                      │                                          │
                                      ▼                                          ▼
                            ┌──────────────────┐                       ┌──────────────────┐
                            │ PostgreSQL / Neon│                       │    Cloudinary    │
                            │ Photo metadata   │                       │ private originals│
                            └──────────────────┘                       │ + web variants   │
                                                                       └──────────────────┘

Admin upload (future):

Admin Browser ── authenticated request ──► Spring Boot
Admin Browser ◄── short-lived signed params ── Spring Boot
Admin Browser ───── direct image upload ─────► Cloudinary
Admin Browser ── publicId + metadata ────────► Spring Boot
Spring Boot ── verify Cloudinary asset ──────► Cloudinary
Spring Boot ── persist trusted metadata ─────► PostgreSQL
```

浏览器读取展示图片时直接访问 Cloudinary CDN 的受控衍生图；Spring Boot 不代理大图片字节。

---

## 3. Technology Stack

| Layer | Technology | Responsibility |
|---|---|---|
| Runtime | Java 21 LTS | Backend runtime |
| Framework | Spring Boot 4.1.x | Application bootstrap and configuration |
| HTTP | Spring Web | REST controllers and JSON |
| Persistence | Spring Data JPA | Photo repository and query mapping |
| Security | Spring Security | Session auth, authorization, CORS, CSRF |
| Validation | Bean Validation | Request boundary validation |
| Operations | Spring Boot Actuator | Health and readiness only |
| Build | Maven Wrapper | Reproducible backend build |
| Database | PostgreSQL on Neon | Authoritative photo metadata |
| Pool | HikariCP | JDBC connection pooling |
| Migration | Flyway | Versioned schema ownership |
| Media | Cloudinary | Original storage, transforms, CDN delivery |
| Backend hosting | Railway | Spring Boot deployment |
| Frontend hosting | Vercel | Next.js deployment; detailed elsewhere |

---

## 4. Monorepo Structure

目标结构：

```text
MyGallery/
├── frontend/
├── backend/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/.../mygallery/
│   │   │   │   ├── photo/
│   │   │   │   ├── admin/
│   │   │   │   ├── auth/
│   │   │   │   ├── media/
│   │   │   │   └── common/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   └── test/java/.../mygallery/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── docs/
├── prototype/
├── README.md
├── AGENTS.md
└── .gitignore
```

这是一个 monorepo 中的 Spring Boot modular monolith；每个包是业务模块，不是独立部署单元。

---

## 5. Backend Architecture

### 5.1 Module responsibilities

| Module | Owns | Must not own |
|---|---|---|
| `photo` | Photo entity, repository, public queries, DTO mapping, archive grouping | Authentication or Cloudinary credentials |
| `admin` | Authenticated photo commands, upload orchestration, admin-only DTOs | Public visibility bypass outside explicit admin use cases |
| `auth` | Security configuration, admin principal, login/logout/session, CSRF | Photo business rules |
| `media` | Cloudinary signature generation, asset verification, delivery URL generation | Photo database writes |
| `common` | Error model, request ID, shared configuration, time abstraction | Feature-specific business logic |

Recommended dependency direction:

```text
admin ─────► photo
  │           │
  └────► media◄┘

auth ─────► common
photo ────► common
media ────► common
```

No module calls another module's controller. Coordination happens through application services/interfaces. JPA entities remain inside the backend and are never serialized as API responses.

### 5.2 Internal layers

Each module may contain a small number of internal packages when implementation begins:

```text
photo/
├── api/             # public controller + request/response DTOs
├── application/     # use cases and transaction boundaries
├── domain/          # PhotoEntity, Visibility, invariants
└── persistence/     # repository and query implementation
```

Do not mirror this structure into dozens of empty classes. Package by feature first; add layers only where a real boundary exists.

### 5.3 Transaction boundaries

- Read endpoints use read-only service transactions where useful
- Creating/updating photo metadata is one database transaction
- Cloudinary and PostgreSQL cannot share an ACID transaction
- Asset verification completes before a Photo row is committed
- Deletion removes the database authority first; Cloudinary cleanup runs after commit as a best-effort call. A failure is logged for operator retry/reconciliation. An orphaned private asset is safer than a public database row pointing to a missing asset

---

## 6. REST API Conventions

Base path: `/api/v1`

| Concern | Convention |
|---|---|
| Media type | `application/json` |
| IDs | UUID string |
| Photo date | ISO 8601 date, `YYYY-MM-DD` |
| Audit timestamps | ISO 8601 UTC instant, for example `2026-08-27T04:30:00Z` |
| Nullable values | Explicit JSON `null`, not missing because of mapper defaults |
| Collection ordering | Deterministic; always includes UUID as final tie-breaker |
| Validation errors | Unified `ApiErrorResponse` with optional `fieldErrors` |
| Unknown/private photo | Public API returns the same `404 PHOTO_NOT_FOUND` response |

Arbitrary field sorting is not exposed. V1 only supports the documented date order, preventing accidental slow queries and keeping the editorial timeline deterministic.

### 6.1 Public API

All public photo queries enforce `visibility = PUBLIC` in the repository/query boundary.

| Method and path | Query | Response | Notes |
|---|---|---|---|
| `GET /api/v1/photos` | `year?`, `month?`, `order=newest\|oldest`, `page=0`, `size=24` | `PhotoPageResponse` | `size` max 100; `month` requires `year` |
| `GET /api/v1/photos/featured` | `limit=12` | `PhotoSummaryResponse[]` | Limit max 30; newest first; frontend chooses Hero randomly from the returned pool |
| `GET /api/v1/photos/{id}` | — | `PhotoResponse` | Returns only a public photo; no original/public ID |
| `GET /api/v1/archive` | `year?` | `ArchiveResponse` | Years, months, photos all newest first; no nested pagination in V1 |

`GET /photos` date filters are converted into date ranges rather than database `EXTRACT` filters, so the `taken_at` index remains usable.

Example list envelope:

```json
{
  "items": [],
  "page": 0,
  "size": 24,
  "totalElements": 0,
  "totalPages": 0
}
```

The frontend API adapter unwraps `items`; components continue to receive `PhotoSummary[]`. The archive response is the preferred source for homepage timeline rendering.

### 6.2 Future admin and auth API

These endpoints are reserved for the future private admin UI; they are not part of the public navigation or current frontend scope.

| Method and path | Purpose |
|---|---|
| `GET /api/v1/admin/csrf` | Start/recover the CSRF handshake |
| `POST /api/v1/admin/session` | Authenticate the one site owner and create a server session |
| `GET /api/v1/admin/session` | Return minimal authenticated-session state |
| `DELETE /api/v1/admin/session` | Logout and invalidate the session |
| `GET /api/v1/admin/photos` | Manage PUBLIC and PRIVATE photos; paginated |
| `GET /api/v1/admin/photos/{id}` | Retrieve editable metadata |
| `POST /api/v1/admin/uploads/signature` | Issue short-lived, tightly scoped signed upload parameters |
| `POST /api/v1/admin/photos` | Verify uploaded asset, then create metadata |
| `PUT /api/v1/admin/photos/{id}` | Replace all mutable metadata; does not replace the asset |
| `DELETE /api/v1/admin/photos/{id}` | Remove metadata and schedule private asset cleanup |

No public create/update/delete endpoint exists. Admin read endpoints are included because private photos cannot be managed through public reads; no additional generic CRUD surface is planned.

### 6.3 DTO boundary

The main DTOs are:

- `PhotoEntity`: persistence only; never leaves the backend
- `PhotoSummaryResponse`: cards, stream, archive, and lightbox navigation
- `PhotoResponse`: one public photo plus description and explicitly allowed EXIF
- `PhotoCreateRequest`: owner-entered metadata plus uploaded `cloudinaryPublicId`
- `PhotoUpdateRequest`: all mutable metadata; no ID, dimensions, public ID, or audit timestamps
- `ArchiveResponse`: `Year → Month → PhotoSummaryResponse`

Exact fields and JSON examples are defined in `docs/DATA_MODEL.md`.

---

## 7. Frontend / Backend Communication

### 7.1 Read flow

```text
Next.js Server Component
    │  GET JSON
    ▼
Spring public controller
    │
    ▼
Photo query service ── visibility = PUBLIC ──► PostgreSQL
    │
    ├── derived year / month / orientation / aspectRatio
    └── signed fixed Cloudinary variant URLs
    ▼
Public response DTO
```

- Next.js Server Components are the primary API consumers
- Browser-side admin requests, when built, use `credentials: "include"`
- The API base URL is environment configuration, never hardcoded
- Backend API secrets never enter `NEXT_PUBLIC_*` variables or browser bundles
- Public API responses never include `visibility`, raw EXIF, `cloudinaryPublicId`, database fields, or original URLs

### 7.2 Cache and freshness

- Public GET responses may use `Cache-Control`/ETag and short shared-cache lifetimes
- Admin/session/upload responses use `Cache-Control: no-store`
- Cloudinary derivative URLs are immutable for a unique public ID and may be cached long-term
- Frontend V1 may use time-based ISR as defined in the frontend design
- A future admin publish action may call a tightly authenticated Vercel on-demand revalidation hook; that hook is not required for V1 and is not a public backend API

API caching must not turn a PRIVATE record into a cached public response. Visibility is checked before DTO creation, and updates from PUBLIC to PRIVATE require public API cache invalidation/short TTL.

---

## 8. Cloudinary Integration

### 8.1 Asset classes

| Asset | Access | Purpose |
|---|---|---|
| Original | Cloudinary `private` delivery type | Preservation and future reprocessing; original requires signed access |
| Thumbnail | Eager/allowlisted named transform, max about 480 px | Small previews |
| Card | Eager/allowlisted named transform, max about 1280 px | Stream and archive cards |
| Display | Eager/allowlisted named transform, max about 2048 px | Hero and lightbox |

Recommended transform behavior:

- `c_limit`/equivalent: never upscale and never exceed the configured bounding box
- automatic format and quality for delivery (`f_auto`, `q_auto` or named equivalents)
- strip embedded metadata from public derivatives
- preserve aspect ratio; only Hero/stream variants may opt into an explicitly named crop
- enable Strict Transformations and eagerly generate or explicitly allow only the predefined named transforms

The backend owns a fixed `MediaUrlFactory`. A client cannot send transformation expressions, dimensions, delivery type, folder, or format to be signed.

### 8.2 Public response rule

Cloudinary `private` delivery protects the original but normally allows derivatives; Strict Transformations is therefore mandatory to stop arbitrary derivative generation. Public derivative URLs may be signed when generated, but a displayed derivative remains shareable and is not treated as a secret.

The public DTO contains only `thumbnailUrl`, `cardUrl`, and `displayUrl`. It never contains:

- original URL
- Cloudinary API secret
- upload signature
- raw `cloudinaryPublicId`
- arbitrary transformation endpoint

A transformed URL can still be copied after it has been rendered. Security relies on keeping the original private and constraining available resolution, not on pretending the browser can hide a displayed resource.

---

## 9. Direct Upload Flow

Spring Boot does not receive the image bytes.

```text
1. Admin gets CSRF token and authenticated session
2. Browser validates obvious file type/size locally
3. POST /admin/uploads/signature with declared name, MIME type, byte size
4. Backend rate-limits and validates the request
5. Backend returns short-lived signed parameters bound to:
   - Cloudinary `private` image delivery type
   - owner-only folder prefix
   - generated unique public ID
   - signed upload preset / allowed formats / maximum size
   - timestamp and expiry
6. Browser uploads directly to Cloudinary
7. Cloudinary returns upload result to the browser
8. Browser POSTs /admin/photos with publicId + editable metadata
9. Backend queries Cloudinary server-to-server and verifies the asset
10. Backend trusts verified width/height/format/bytes, then inserts Photo
```

The create request does not make client-supplied width, height, format, URL, or EXIF authoritative. The backend verifies that the asset:

- belongs to the configured Cloudinary account and folder
- uses the required Cloudinary `private` delivery type
- is an image in the allowlisted decoded formats
- is within configured byte and dimension limits
- has not already been attached to another Photo

The admin UI treats issued upload parameters as short-lived (target use window: 60–120 seconds). Because provider-side signature validity may be longer, a generated unique public ID plus `overwrite=false` supplies the replay boundary. All security-sensitive parameters are signed, and the API secret remains server-side.

---

## 10. PostgreSQL / Neon

### 10.1 Ownership

PostgreSQL is authoritative for title, date, visibility, featured state, approved EXIF, and the mapping to the private Cloudinary asset. Cloudinary metadata never becomes the publishing authority.

### 10.2 Connection configuration

- Use Neon TLS connections in production
- Use HikariCP with a small pool suitable for Railway and the Neon plan; start conservatively and tune from metrics
- Prefer the Neon pooled application endpoint where appropriate
- Keep `spring.jpa.open-in-view=false`
- Never log JDBC URLs containing credentials
- Run a connectivity/readiness check without exposing database details publicly

### 10.3 Schema management with Flyway

- `src/main/resources/db/migration/V1__create_photos.sql` owns the initial schema
- Production Hibernate setting is `ddl-auto=validate` (or equivalent validation-only behavior)
- `ddl-auto=create`, `create-drop`, and `update` are prohibited in production
- Flyway runs before JPA initialization
- Every schema change is a new immutable migration; committed migrations are never edited after deployment
- Production migration credentials may be separated from runtime credentials later; V1 may use one restricted owner credential if operational simplicity requires it

Data model and indexes are specified in `docs/DATA_MODEL.md`.

---

## 11. Error Handling

All controllers use one error contract:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "status": 400,
  "timestamp": "2026-08-27T04:30:00Z",
  "path": "/api/v1/admin/photos",
  "requestId": "01J6...",
  "fieldErrors": [
    { "field": "title", "code": "Size", "message": "size must be between 0 and 200" }
  ]
}
```

Recommended stable codes include:

| HTTP | Code | Use |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Invalid shape or field values |
| 400 | `INVALID_FILTER` | Invalid year/month/order combination |
| 401 | `AUTHENTICATION_REQUIRED` / `INVALID_CREDENTIALS` | Missing session or failed login |
| 403 | `ACCESS_DENIED` / `CSRF_INVALID` | Authenticated but forbidden or invalid CSRF |
| 404 | `PHOTO_NOT_FOUND` | Missing or non-public photo on public API |
| 409 | `PHOTO_ASSET_ALREADY_USED` | Duplicate public ID / conflicting write |
| 413 | `UPLOAD_TOO_LARGE` | Declared or verified upload exceeds limit |
| 429 | `RATE_LIMITED` | Include `Retry-After` |
| 500 | `INTERNAL_ERROR` | Sanitized unexpected failure |

Production responses never contain a stack trace, exception class, SQL, credentials, internal path, Cloudinary signature, or upstream raw error. Full diagnostics stay in protected logs and are correlated by `requestId`.

---

## 12. Environment Configuration

Secrets are injected through Railway/Neon/Cloudinary environment management and never committed. The future root or backend `.env.example` contains names with empty values only:

```dotenv
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
MIGRATION_DATABASE_URL=

CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_UPLOAD_FOLDER=
CLOUDINARY_UPLOAD_PRESET=

ADMIN_USERNAME=
ADMIN_PASSWORD_BCRYPT_HASH=
ADMIN_SESSION_TIMEOUT=

CORS_ALLOWED_ORIGINS=
PUBLIC_WEB_ORIGIN=

MAX_UPLOAD_BYTES=
SPRING_PROFILES_ACTIVE=
```

Notes:

- If `DATABASE_URL` includes credentials, separate username/password variables may be omitted by the deployment adapter; never commit either representation
- `ADMIN_PASSWORD_BCRYPT_HASH` is generated offline; plaintext admin passwords are not stored in environment or source
- `CORS_ALLOWED_ORIGINS` is an explicit comma-separated allowlist, not `*`
- Frontend-specific variables live under `frontend/`; Cloudinary secrets never do
- Local `.env` files remain untracked

---

## 13. Deployment

### 13.1 Railway backend

- Service root directory: `backend/`
- Build using the committed Maven Wrapper and Java 21
- Start the executable Spring Boot jar using Railway's assigned `PORT`
- Production profile enables TLS-aware forwarded headers, secure cookies, Flyway, validation-only JPA, and sanitized errors
- Expose only a minimal Actuator health/readiness endpoint; do not publicly expose `env`, `configprops`, `beans`, `mappings`, heap dumps, or loggers
- Configure a custom `api.example.com` domain and HTTPS

### 13.2 Neon database

- PostgreSQL is reachable only through TLS credentials stored in Railway
- Use a production branch/database separate from local/test databases
- Enable the Neon plan's available backups/PITR and test restoration before launch

### 13.3 Cloudinary

- Use a production environment/folder separate from development uploads
- Apply `private` originals, Strict Transformations, eager/allowlisted named variants, upload constraints, and credential rotation
- Only `CLOUDINARY_CLOUD_NAME` and fixed delivered URLs may appear client-side; API key/secret stay in Railway

### 13.4 Vercel frontend

- Service root directory: `frontend/`
- `www.example.com` calls `https://api.example.com`
- Exact frontend deployment details remain owned by the frontend technical design

---

## 14. Testing Strategy

Use JUnit 5 and Spring Boot Test. Prefer high-value behavior coverage over a coverage percentage target.

| Layer | Test focus |
|---|---|
| Domain/service unit | Derived orientation/aspect ratio, update invariants, archive grouping/order |
| Repository integration | PUBLIC filtering, date-range filters, featured query, deterministic sort, constraints |
| Controller/API | DTO shape, pagination bounds, validation errors, `404` privacy behavior |
| Security | anonymous public GET, admin rejection, login/session, CSRF, CORS allowlist, secure headers |
| Media adapter | Signature binds fixed params; URL factory only emits named capped variants; upload verification rejects mismatches |
| Migration | Empty PostgreSQL schema migrates successfully; JPA validation matches Flyway schema |

Database behavior tests should run against real PostgreSQL semantics (for example Testcontainers during implementation), not rely solely on H2, because partial indexes, UUID, timestamps, and constraints are PostgreSQL-specific.

Critical acceptance scenarios:

1. PRIVATE photos never appear in list, featured, detail, or archive responses
2. Years, months, and photos are consistently newest first
3. A public detail request for a PRIVATE UUID returns the same 404 as an unknown UUID
4. Anonymous and CSRF-invalid requests cannot mutate admin data or obtain upload signatures
5. Invalid dates/dimensions/EXIF and oversized uploads fail safely
6. Public responses contain no original URL, public ID, raw EXIF, GPS, or secrets

---

## 15. Observability and Operations

- Structured application logs include request ID, route template, status, latency, and authenticated admin boolean—not credentials or raw cookies
- Log admin create/update/delete events with photo ID and action, but no password, signature, JDBC URL, API secret, or raw EXIF
- Monitor 5xx rate, response latency, database pool saturation, Cloudinary verification failures, repeated failed logins, and rate-limit events
- Health responses reveal only `UP`/`DOWN`; detailed components are visible only to operators
- Alerting and distributed tracing are production hardening, not prerequisites for the V1 domain model

---

## 16. Repository and Step Boundary

Before implementation, `.gitignore` must cover:

```gitignore
.env
.env.*
!.env.example
node_modules/
.next/
target/
.DS_Store
*.log
testphoto/
```

Existing `testphoto/` content is not deleted by this design step. Step 3B creates documentation only; backend scaffolding, database creation, Cloudinary configuration, deployment, frontend changes, and Step 4 implementation are explicitly deferred.
