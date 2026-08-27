# MyGallery — Security Design

**Version:** 0.1  
**Status:** Draft  
**Scope:** Public photo API, one-owner admin API, Cloudinary media, Neon PostgreSQL

---

## 1. Security Objectives

The V1 security objective is:

```text
original asset protection
+ resolution control
+ admin protection
+ scraping cost increase
```

It is not DRM and it does not promise that a visible photograph cannot be copied.

> No website can completely prevent screenshots  
> or copying images already rendered in the browser.

Security success means:

- original high-resolution files are not publicly retrievable
- public visitors receive only capped web variants
- only the owner can create, edit, publish, delete, or sign uploads
- private Photo rows do not leak through any public endpoint
- secrets and private metadata remain server-side
- automated abuse is made more expensive and observable

---

## 2. Threat Model

### 2.1 Assets

| Asset | Security property |
|---|---|
| Original photographs | Confidentiality and integrity |
| Public derivatives | Integrity and controlled maximum resolution; confidentiality is not possible after display |
| Photo metadata/visibility | Integrity; private row confidentiality |
| Admin password/session | Confidentiality and authentication integrity |
| Cloudinary API secret/upload authority | Confidentiality and least privilege |
| Neon credentials/data | Confidentiality, integrity, availability |
| Service configuration | Confidentiality and integrity |

### 2.2 Relevant attackers and failures

| Threat | Example | Primary controls |
|---|---|---|
| Casual original discovery | Modify a displayed URL to request 6000 px or the original | Private originals, Strict Transformations, fixed variants, no original URL |
| Automated scraping | Crawl archive and download every visible derivative | Resolution cap, caching, rate limits, bot/edge hardening, monitoring |
| Credential brute force | Repeated login attempts | Strong BCrypt password, generic errors, login rate limit, logs |
| CSRF | Malicious site causes owner browser to publish/delete | CSRF token, SameSite cookie, exact CORS, Origin checks |
| Session theft | Script/network attacker obtains session ID | HttpOnly, Secure, HTTPS/HSTS, short idle timeout, no session in JS storage |
| Authorization bypass | Anonymous caller invokes upload signature/admin CRUD | Deny-by-default security rules plus method authorization tests |
| Upload tampering | Change signed folder/type/transform or attach another asset | Sign exact parameters, unique ID, overwrite false, server-side asset verification |
| Malicious/invalid file | Oversized file, SVG/script, mislabeled MIME | Upload preset limits, decoded format allowlist, Cloudinary verification |
| Metadata privacy leak | GPS/serial numbers returned as EXIF | Allowlisted fields only, strip public derivative metadata, no raw EXIF |
| Secret leakage | Key committed, logged, or shipped to browser | Environment secrets, log redaction, gitignore, secret rotation |
| SQL/input attacks | Invalid filters or content reaches persistence | Bound JPA parameters, Bean Validation, length/range limits, no dynamic SQL sort |
| Error disclosure | Stack trace or upstream response reveals internals | Unified sanitized errors, protected logs |
| Resource exhaustion | Transform spam, unbounded pages, connection exhaustion | Strict transforms, page caps, small DB pool, request/upload/rate limits |

### 2.3 Explicit limitations

- Public web variants can be saved, shared, re-encoded, or screenshotted
- A signed delivery URL displayed in a browser can be copied; signing proves authorization/URL integrity, not viewer identity
- `robots.txt`, disabled right-click, transparent overlays, hidden DOM, and Referer checks are not image protection
- V1 in-memory rate limits and sessions assume one Railway application instance; they are not distributed controls
- Large volumetric DDoS must be handled by the hosting/CDN edge, not by Spring Boot alone

---

## 3. Admin Authentication

### 3.1 V1 identity model

There is exactly one site owner. V1 does not create a user table and does not support:

- registration
- password reset email
- public accounts
- social login or OAuth
- role management UI
- multiple organizations

Configuration:

```text
ADMIN_USERNAME
ADMIN_PASSWORD_BCRYPT_HASH
```

The BCrypt hash is generated offline with a cost calibrated for the production CPU. Plaintext credentials are never stored in source, `.env.example`, logs, database, browser storage, or deployment variables. Password rotation replaces the hash in Railway secrets and restarts/redeploys the backend.

Spring Security loads one in-memory `UserDetails` principal with internal authority `ROLE_ADMIN`. The authority has no public API representation and no management UI.

### 3.2 JSON session flow

```text
1. Admin UI GET /api/v1/admin/csrf with credentials included
2. API creates/reuses a server session and returns a CSRF token in JSON
3. Admin UI POST /api/v1/admin/session
   - JSON username/password
   - X-CSRF-TOKEN header
   - credentials: include
4. Spring Security verifies BCrypt and rotates the session ID
5. Browser receives only an opaque HttpOnly session cookie
6. Every later admin mutation includes the current CSRF header
7. DELETE /api/v1/admin/session invalidates the session and clears the cookie
```

Failed login uses a generic `401 INVALID_CREDENTIALS`; it does not reveal whether the username or password was wrong. Authentication failures are rate-limited and logged without the submitted password.

HTTP Basic, default HTML form login, remember-me, and JWT authentication are disabled. In particular, no JWT or session credential is stored in `localStorage` or `sessionStorage`.

### 3.3 Server-side session lifecycle

- `SessionCreationPolicy.IF_REQUIRED`
- migrate/change session ID on successful authentication to prevent fixation
- target idle timeout: 30 minutes, configurable
- target absolute admin session lifetime: one working day or less
- one concurrent admin session may be enforced; a new login invalidates the previous session
- logout invalidates server state and deletes the cookie
- authentication/session responses use `Cache-Control: no-store`

V1 uses the servlet container's server-side session store on one Railway instance. A restart logging the owner out is acceptable. Before horizontal scaling, move sessions to a deliberately selected shared server-side store (for example JDBC-backed Spring Session) or enforce sticky single-instance operation; do not add Redis merely for hypothetical scale.

---

## 4. Authorization

### 4.1 Route policy

```text
PERMIT ANONYMOUS
GET  /api/v1/photos
GET  /api/v1/photos/featured
GET  /api/v1/photos/{id}
GET  /api/v1/archive
GET  /api/v1/admin/csrf
POST /api/v1/admin/session       # still CSRF-protected and rate-limited
GET  /actuator/health            # sanitized status only

ROLE_ADMIN REQUIRED
GET/DELETE /api/v1/admin/session (except login endpoint above)
ALL        /api/v1/admin/photos/**
POST       /api/v1/admin/uploads/signature

DENY
Everything not explicitly matched
```

Exact matcher ordering is tested so `/api/v1/photos/**` cannot accidentally include an admin mutation and a broad permit rule cannot shadow `/api/v1/admin/**`.

### 4.2 Defense in depth

- URL rules require `ROLE_ADMIN`
- Admin application service methods also use method-level authorization where appropriate
- Public repositories/query services always include `visibility = PUBLIC`
- A public detail query uses `findPublicById`, not unrestricted `findById` plus controller filtering
- PRIVATE and nonexistent IDs both return public 404
- Admin DTOs and public DTOs are separate types
- Cloudinary upload signature creation is available only through an authenticated, CSRF-protected service method

Authentication proves identity; it does not allow bypassing validation, upload constraints, visibility invariants, or asset verification.

---

## 5. Cookie Security

Production session cookie:

```text
Name:     __Host-mygallery-session
Secure:   true
HttpOnly: true
SameSite: Lax
Path:     /
Domain:   not set (host-only for api.example.com)
```

Why this works for the planned domains:

- `https://www.example.com` and `https://api.example.com` are cross-origin but same-site
- `SameSite=Lax` allows the intended same-site relationship without enabling general cross-site cookie use
- a host-only `__Host-` cookie cannot be set for a parent domain and is never sent to the frontend host
- `HttpOnly` prevents frontend JavaScript from reading the session ID
- `Secure` and HTTPS prevent cleartext transport

The browser must use `credentials: "include"` for admin fetches. The API must return `Access-Control-Allow-Credentials: true` only for an allowed origin.

Vercel and Railway default preview domains are cross-site. Do not weaken production cookies globally for previews. Prefer custom staging subdomains under one registrable domain or a same-origin server proxy. If a specific environment truly requires `SameSite=None`, it must also use `Secure`, an exact isolated origin, and the full CSRF design.

No sensitive metadata, role, password hash, or API token is placed inside the cookie. It carries only a high-entropy opaque session identifier.

---

## 6. CORS

CORS is an explicit environment allowlist, for example:

```text
CORS_ALLOWED_ORIGINS=https://www.example.com
```

Production behavior:

| Setting | Value |
|---|---|
| Allowed origins | Exact configured HTTPS origins only |
| Allow credentials | `true` |
| Methods | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Request headers | `Content-Type`, `X-CSRF-TOKEN`, request ID header if used |
| Exposed headers | `ETag`, `Retry-After`, request ID if needed |
| Preflight cache | Short bounded value, for example one hour |
| Response variance | `Vary: Origin` |

Never combine credentialed requests with `Access-Control-Allow-Origin: *`. Do not use a permissive `*.vercel.app` origin pattern: another deployment/account must not become a trusted admin origin. Local development origins are configured only in the dev profile, such as an exact `http://localhost:3000`.

CORS is not authentication and does not stop scripts, command-line clients, or server-to-server callers. Admin session authorization and CSRF remain mandatory.

---

## 7. CSRF

Session cookies are ambient browser credentials, so all state-changing admin endpoints remain under Spring Security CSRF protection.

Design:

- `GET /api/v1/admin/csrf` returns `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`
- the token is associated with the current server session
- the future admin frontend keeps the token in memory and sends it in the named header
- on reload/session change/403 CSRF failure, the frontend requests a new token
- login and logout are CSRF-protected, preventing login CSRF and forced logout
- public GET endpoints remain safe/idempotent and never mutate view counters or other state
- do not exempt the entire `/api/v1/admin/**` path from CSRF

The session cookie stays HttpOnly. Returning the separate CSRF token in JSON avoids making the session credential readable and works across the planned same-site subdomains. CORS and an allowed-Origin/Referer check on admin mutations provide additional defense; the CSRF token is still the primary control.

---

## 8. Input and Data Security

- Use request DTOs with Bean Validation; never bind request JSON directly into `PhotoEntity`
- Trim strings, normalize blank optional fields to null, and enforce documented length limits
- Validate `takenAt` as a real, non-future calendar date
- Validate year/month/page/size against bounded numeric ranges
- Use an enum allowlist for visibility; unknown values fail with 400
- Use numeric ranges for EXIF and do not parse arbitrary display strings into SQL
- Use Spring Data bound parameters; no concatenated SQL and no arbitrary client-provided sort property
- Reject unknown JSON properties for admin write requests or otherwise test that they cannot change internal fields
- Apply JSON/body size limits substantially below media upload sizes; image bytes never enter these endpoints
- Never accept a client URL and ask Cloudinary/server code to fetch it, avoiding SSRF

API updates cannot set `id`, `createdAt`, `updatedAt`, verified dimensions, folder, delivery type, transformation, or Cloudinary credentials.

---

## 9. Cloudinary Original Protection

### 9.1 Chosen V1 policy

Original uploads use Cloudinary delivery `type=private`.

```text
Private original (for example 6000×4000)
    ├── not returned by public API
    ├── not embedded in HTML/Next.js data
    ├── not stored in Git/PostgreSQL/frontend
    └── accessible only through explicitly generated signed admin access

Strict Transformations enabled
    ├── gallery_thumbnail: max about 480 px
    ├── gallery_card:      max about 1280 px
    └── gallery_display:   max about 2048 px
```

Cloudinary private delivery alone protects the original but normally permits derived assets. Therefore V1 also enables Strict Transformations and eagerly generates or explicitly allowlists only the named gallery variants. Arbitrary unsigned transformations such as changing `w_2048` to `w_6000` must fail.

Named variants:

- use a limit operation and never upscale
- encode a hard maximum width/height
- use automatic delivery format/quality as compatible with strict variant configuration
- strip EXIF/profile metadata from the public derivative
- preserve aspect ratio except for separately named editorial Hero/stream crops
- are generated/configured only by trusted backend/Cloudinary settings

For rare owner-only original download, the backend may create a time-limited signed private download URL after fresh admin authorization. Such a URL is `no-store`, short-lived, never returned by public endpoints, and never embedded in public pages.

### 9.2 What signatures do and do not do

- upload signatures prove that the server authorized a fixed upload parameter set
- delivery signatures authorize protected asset/transform access and prevent URL tampering
- signatures do not stop someone from sharing a public derivative URL after seeing it
- a signature embedded in a visible URL is not an API secret; the Cloudinary API secret that generated it must remain server-side
- the backend never accepts client-provided transformation strings to sign

Do not proxy originals through Spring Boot and do not treat `next/image` proxying, obscured URLs, right-click blocking, Referer checks, or a robots rule as the original-access boundary.

---

## 10. Upload Signature Security

### 10.1 Signature endpoint

`POST /api/v1/admin/uploads/signature` requires:

- valid `ROLE_ADMIN` session
- valid CSRF token
- allowed CORS Origin
- upload-specific rate limit
- declared file name, MIME type, and byte size within configured bounds

The backend generates and signs the security-sensitive parameters itself:

```text
resource_type=image
type=private
folder=<fixed owner-only folder>
public_id=<server-generated unique opaque value>
overwrite=false
timestamp=<current server time>
upload_preset=<signed restricted preset>
eager=<fixed named derivative set, if generated during upload>
```

The response includes only values the Cloudinary browser upload requires: cloud name, API key (identifier, not secret), timestamp, signature, preset/folder/public ID, and a short client expiry hint. It never includes the API secret.

### 10.2 Replay and tampering controls

- timestamped signatures are used immediately; the UI discards an expired response
- the unique generated public ID plus `overwrite=false` prevents a replay from replacing an existing original
- folder, delivery type, eager variants, and preset are inside the signed parameter set; the client cannot safely alter them
- signature creation uses the official Cloudinary SDK and a server clock synchronized by the platform
- signatures and full signed request bodies are not logged

Provider-side signature validity may be longer than the UI's 60–120 second target, so uniqueness and no-overwrite remain necessary replay controls.

### 10.3 Upload constraints and completion verification

The signed Cloudinary upload preset restricts:

- maximum bytes appropriate to the current Cloudinary plan
- `resource_type=image`
- decoded format allowlist (for example JPEG/PNG/WebP/approved HEIC support)
- no SVG, video, raw archive, or PDF in V1
- owner-only folder and private delivery type

Declared MIME type and file extension are hints, not trust anchors. Before inserting a Photo row, Spring Boot queries Cloudinary server-to-server and verifies:

- expected account, folder prefix, public ID, `resource_type`, and `type=private`
- decoded format is allowlisted
- bytes/dimensions are positive and within policy
- asset exists and is not already linked to another Photo
- required derivatives were generated/allowed successfully

The backend obtains `width` and `height` from this verified asset. Raw Cloudinary response metadata and EXIF are not copied wholesale. An invalid orphan upload is kept private and scheduled for deletion; it is never published.

---

## 11. EXIF and Location Privacy

The persistence/public allowlist is limited to camera, lens, capture date, ISO, aperture, shutter speed, and focal length. The administrator reviews candidate EXIF before saving it.

Never save or expose by default:

- GPS coordinates, altitude, or location track
- body/lens serial numbers
- device owner name
- embedded thumbnail
- maker notes, comments, filesystem path, editing history
- a raw EXIF JSON map

The public location string is owner-authored display text, not automatically copied GPS. Public derivatives strip metadata even when the private original retains it.

---

## 12. Rate Limiting

Redis is not introduced in V1. A bounded in-memory token-bucket/fixed-window filter on the single Railway instance provides a first line of abuse control. Initial values are configuration, not hardcoded security guarantees:

| Target | Example starting policy | Key |
|---|---|---|
| Admin login | 5 attempts / 15 minutes | Source IP + username hash |
| CSRF/session bootstrap | 30 / minute | Source IP |
| Upload signature | 10 / minute | Authenticated session + source IP |
| Admin writes | 60 / minute | Authenticated session |
| Public API | 120 / minute, with a small burst | Source IP |

Requirements:

- return `429 RATE_LIMITED` and `Retry-After`
- cap and expire the in-memory key map so attackers cannot exhaust heap with unique keys
- do not log raw usernames, session IDs, or full IPs longer than operationally necessary
- trust forwarded client IP headers only from Railway's known proxy chain; otherwise use the direct peer address
- public CDN/API caching should absorb repeat reads before they consume database capacity

Limitations: counters reset on restart and are per instance. Before scaling to multiple instances, move enforcement to a trusted edge/gateway or a shared limiter. Redis remains unnecessary until distributed enforcement is actually required.

Scraping-specific production hardening may add edge bot controls, bandwidth alerts, graduated throttling, or optional watermark variants. These increase cost; they do not make rendered images uncopyable.

---

## 13. Secrets and Infrastructure Security

### 13.1 Secret inventory

Treat these as secrets even when one is a hash or URL:

- Neon connection URL/username/password
- Cloudinary API secret and API key
- admin BCrypt password hash and username
- future revalidation/webhook secrets
- any future session-signing key (not required for opaque server sessions)

`CLOUDINARY_CLOUD_NAME` and allowed origins are configuration, not credentials, but still come from the environment to avoid hardcoded deployment coupling.

### 13.2 Handling rules

- store production values in Railway/Neon/Cloudinary secret managers
- commit only `.env.example` with empty variable values
- `.env` and `.env.*` are ignored except `.env.example`
- never paste secrets into issues, commits, screenshots, test fixtures, or logs
- redact authorization/cookie headers, upload signatures, password fields, and credential-bearing URLs
- use separate development and production Cloudinary folders/environments and databases
- rotate immediately if a secret is exposed; deleting the git line is not sufficient
- restrict who can access Railway, Neon, Cloudinary, Vercel, and the source repository; enable MFA on those operator accounts

Database least privilege:

- the runtime credential only accesses the application schema
- a separate Flyway migration credential is preferred as operations mature
- enforce TLS to Neon
- backups/PITR and a tested restore procedure protect against accidental deletion; a backup is not an access-control substitute

---

## 14. Error and Logging Security

Production API errors use the unified contract in `docs/TECHNICAL_DESIGN.md` and never return:

- stack traces or exception class names
- SQL text, constraint details, table/column names
- filesystem/classpath locations
- session IDs, cookies, credentials, signatures, or public original links
- raw Cloudinary/Neon provider responses
- private photo existence information

Unexpected exceptions return generic `500 INTERNAL_ERROR` plus a request ID. Protected structured logs retain the exception and request correlation, with secret redaction.

Audit-worthy events:

- successful and failed login (generic identity signal, no password)
- logout/session invalidation
- upload signature issuance outcome (not the signature)
- photo create/update/delete and visibility changes
- Cloudinary verification/deletion failure
- repeated 401/403/429 patterns

Logs have bounded retention and are access-controlled. They do not become an alternate metadata database.

---

## 15. HTTP and Operational Hardening

- HTTPS only in production; configure Railway forwarded headers correctly before evaluating scheme/client IP
- redirect HTTP to HTTPS at the edge and emit HSTS after custom-domain HTTPS is verified
- API responses set `X-Content-Type-Options: nosniff` and an appropriate restrictive referrer policy
- frontend owns its CSP and clickjacking policy; the API returns JSON and does not embed admin HTML
- Actuator exposes only sanitized health/readiness publicly; no environment, mappings, heap dump, loggers, or configuration endpoints
- admin and authentication responses use `Cache-Control: no-store`
- set bounded JSON request, header, connection, and server timeouts
- use a conservative Hikari pool and monitor saturation to avoid exhausting Neon connections
- keep Java/Spring/Cloudinary SDK dependencies patched; review security advisories before deployment

---

## 16. Security Testing

Required automated tests:

1. Anonymous public GET succeeds; every admin read/write/signature endpoint rejects it
2. Correct login with CSRF creates a session and rotates the ID; incorrect credentials return generic 401
3. Admin mutation without/mismatched CSRF returns 403 and changes nothing
4. Disallowed CORS origin receives no credentialed access headers
5. PUBLIC-to-PRIVATE immediately disappears from list, featured, detail, and archive queries
6. PRIVATE detail and unknown detail return indistinguishable public 404 responses
7. Public DTO serialization never contains visibility, public ID, original URL, GPS, raw EXIF, or audit internals
8. Upload signature binds private type, fixed folder/public ID, no-overwrite, preset, timestamp, and variants
9. Tampered/expired upload parameters and wrong-folder/public-type/oversized assets cannot create a Photo
10. Rate-limited endpoints return sanitized 429 with `Retry-After`
11. Validation and unexpected errors contain no stack trace, SQL, credentials, internal paths, or upstream body
12. Production cookie has `Secure`, `HttpOnly`, `SameSite=Lax`, host-only `__Host-` semantics, and expected path

Manual launch tests should also attempt direct original access and modified 6000 px transformation URLs from a logged-out browser. Both must fail while the three intended web variants remain usable.

---

## 17. Production Checklist

### Application and admin

- [ ] Strong unique admin password hashed with calibrated BCrypt
- [ ] Default form login, HTTP Basic, remember-me, JWT/localStorage auth disabled
- [ ] Session fixation protection, timeout, logout invalidation, and optional one-session limit verified
- [ ] Every admin endpoint protected by role and CSRF; deny-by-default fallback active
- [ ] Login and upload signature rate limits enabled
- [ ] Admin/session responses marked `no-store`

### Cookies, CORS, and HTTPS

- [ ] `www.example.com` and `api.example.com` HTTPS certificates valid
- [ ] `__Host-mygallery-session` is Secure, HttpOnly, SameSite=Lax, Path=/, no Domain
- [ ] Production allowed-origin list contains only exact owned HTTPS origins
- [ ] Credentialed wildcard CORS absent; dev origins absent from production
- [ ] Forwarded-header trust, HTTPS redirect, and HSTS tested behind Railway

### Cloudinary and uploads

- [ ] Originals upload with `type=private`; direct original URL fails anonymously
- [ ] Strict Transformations enabled
- [ ] Only thumbnail/card/display named transforms are eager/allowlisted
- [ ] Modified high-resolution or arbitrary transformation URL fails
- [ ] Public derivatives capped, never upscaled, and stripped of metadata
- [ ] Signature endpoint binds folder, unique public ID, private type, no-overwrite, timestamp, preset, and variants
- [ ] File size/format constraints and server-side completion verification active
- [ ] Cloudinary API secret absent from frontend bundles, responses, source, and logs

### Data and operations

- [ ] Flyway owns schema; production Hibernate is validation-only
- [ ] Neon TLS, least-privilege credentials, backup/PITR, and restore test complete
- [ ] PUBLIC filtering and private-404 behavior covered by repository/API tests
- [ ] Actuator public exposure limited to sanitized health/readiness
- [ ] Logs redact cookies, passwords, signatures, secrets, raw EXIF, and credential URLs
- [ ] `.env`, build output, logs, and `testphoto/` are ignored without deleting existing test images
- [ ] Production error responses manually checked for stack traces and provider leakage
- [ ] Secret scan and dependency/security review completed before launch

---

## 18. Implementation References

- [Cloudinary: media access control and restrictive delivery types](https://cloudinary.com/documentation/control_access_to_media)
- [Cloudinary: upload parameters, private assets, and Strict Transformations](https://cloudinary.com/documentation/upload_parameters)
- [Cloudinary: signed delivery URLs](https://cloudinary.com/documentation/delivery_url_signatures)
- [Cloudinary: Upload API reference](https://cloudinary.com/documentation/image_upload_api_reference)

These references guide implementation details; the repository's tests and the production checklist remain the acceptance criteria.
