# MyGallery Agent Development Rules

## 1. First Rule

> Before implementing any task, read the relevant project documentation.

Required reading priority:

1. Every task: `README.md`, then `docs/PRD.md`
2. Frontend task: also read `docs/USER_FLOW.md` and `docs/FRONTEND_TECHNICAL_DESIGN.md`
3. Backend/API/database task: also read `docs/TECHNICAL_DESIGN.md` and `docs/DATA_MODEL.md`
4. Security/auth/upload task: also read `docs/SECURITY.md`
5. Visual/layout task: also inspect `prototype/` as the Visual Direction Reference

Read the selected documents completely before changing files. If a request conflicts with frozen documentation, do not silently choose a new architecture; identify the conflict and ask for an explicit decision when it materially changes scope.

## 2. Scope Discipline

Implement only the feature explicitly requested.

> Make the smallest coherent change that completes the requested task.

For example, a request to implement Hero does not authorize implementing Archive, adding Admin, changing the database, refactoring unrelated backend code, adding authentication, or redesigning About.

Before editing:

- inspect the current worktree and preserve existing user changes
- identify the smallest set of files that owns the requested behavior
- confirm whether the task is frontend, backend, security/media, documentation, or a deliberate cross-boundary change
- do not create placeholder modules or speculative abstractions for future features

## 3. No Unrelated Refactors

Do not refactor unrelated code because it could be "improved." Refactor outside the requested feature only when:

- the requested feature cannot be completed safely without it
- there is a concrete bug directly blocking the feature
- the user explicitly requested the refactor

When a required refactor expands scope, state why it is necessary. Preserve established naming, structure, and behavior everywhere else.

## 4. Locked Architecture

Do not change these choices without explicit approval:

```text
Frontend:       Next.js 16, React, TypeScript, App Router, CSS Modules
Backend:        Java 21 LTS, Spring Boot 4.1.x, Maven Wrapper
API:            REST + JSON under /api/v1/*
Database:       PostgreSQL on Neon, Spring Data JPA, Flyway
Media:          Cloudinary
Deployment:     Vercel + Railway
Repository:     Monorepo
Backend shape:  Spring Boot modular monolith
```

Do not independently introduce:

- Tailwind
- Redux or Zustand
- Framer Motion, GSAP, or another heavy animation framework
- GraphQL
- Redis or Kafka
- microservices
- Elasticsearch
- Docker orchestration complexity
- Kubernetes
- a CMS or complex OAuth system

If a new requirement genuinely needs a dependency, explain the requirement, why existing tools are insufficient, expected cost/security impact, and the smallest viable option before adding it.

## 5. Frontend Rules

- Use Server Components by default
- Add Client Components only where browser interaction requires them
- Keep every client boundary as small and serializable as possible
- Never turn the whole Homepage or root layout into a `"use client"` component
- Fetch public data through the Spring Boot REST API; never connect to PostgreSQL from Next.js
- Keep API fetch and response adaptation inside `frontend/lib/api/`
- Preserve the formal `PhotoImage` shape: `thumbnailUrl`, `cardUrl`, `displayUrl`
- Do not reintroduce a single `imageUrl` model or construct arbitrary Cloudinary transformations

Required component boundaries:

```text
Hero
└── Server structure + thin HeroPicker Client boundary

PhotoCard
└── Server Component
    └── thin PhotoLightboxTrigger Client boundary

Lightbox
└── Client Component
```

The random Hero selection is presentation randomness: the Server Component renders a deterministic fallback, then the thin `HeroPicker` selects from the featured pool after hydration. Do not make the entire page dynamic/client-rendered to achieve randomness.

## 6. Styling and Visual Rules

Use CSS Modules for component visuals.

Global CSS is limited to:

- reset
- design tokens
- fonts
- base typography

Place component-specific styling in `Component.module.css` next to the component.

Preserve the established visual language:

- generous whitespace
- editorial asymmetry
- original image composition
- restrained typography and interaction
- subtle motion with reduced-motion support

Do not turn the site into a generic equal-cell gallery, SaaS interface, card dashboard, or template portfolio. Do not add heavy animation to compensate for weak layout.

## 7. Photography and Media Rules

- Archive and Lightbox must preserve the original composition by default
- Do not apply a global `object-fit: cover` that crops photographic work
- Hero and explicitly designed editorial slots may use intentional artistic cropping
- Reserve image layout space from the response `aspectRatio` to avoid CLS
- Use `cardUrl` for Archive/Selected Frames and `displayUrl` for Hero/Lightbox unless the technical design is explicitly changed
- Never commit production photographs or original high-resolution assets to GitHub
- Only explicitly approved test assets may exist in Git

`prototype/` is a Visual Direction Reference. After Step 5 begins, production work belongs in `frontend/`. Modify the prototype only when the user explicitly requests prototype work; never copy its Vanilla JS directly into formal Next.js code.

## 8. Backend and API Rules

- Keep the backend a modular monolith; modules are package boundaries, not services
- Spring Boot is the business and visibility authority
- Public endpoints use `/api/v1/*`, REST, and JSON
- Public Photo queries enforce `visibility = PUBLIC` in the repository/query boundary
- PRIVATE and unknown Photo IDs return the same public 404 behavior
- Use bounded pagination/filter values and deterministic ordering
- Use Bean Validation on request DTOs and bound persistence parameters
- Use Flyway for every schema change
- Production JPA is validation-only; never use `ddl-auto=create`, `create-drop`, or `update`
- Do not proxy large upload bytes through Spring Boot

The frontend must never receive:

- database credentials
- `CLOUDINARY_API_SECRET`
- raw `cloudinaryPublicId`
- original image URLs
- PRIVATE Photo data
- raw EXIF or GPS metadata

## 9. DTO and Data Model Rules

Never serialize a JPA entity directly to the frontend. Use explicit request and response DTOs.

The formal public image contract is:

```typescript
interface PhotoImage {
  thumbnailUrl: string;
  cardUrl: string;
  displayUrl: string;
}
```

V1 has one persisted core domain: `Photo`.

Do not pre-create `User`, `Role`, `Tag`, `Category`, `Album`, `Archive`, or EXIF entities without an approved product requirement. The single admin identity comes from secure configuration, not a V1 user table.

Do not persist these derived fields:

- `year` from `takenAt`
- `month` from `takenAt`
- `orientation` from `width` and `height`
- `aspectRatio` from `width` and `height`

`width`, `height`, format, and asset identity come from server-side Cloudinary verification, not browser claims.

## 10. Security Rules

The complete security authority is `docs/SECURITY.md`. Never:

- disable CSRF globally or exempt the entire admin API
- use credentialed `Access-Control-Allow-Origin: *`
- store JWT/session credentials in `localStorage` or `sessionStorage`
- commit secrets, production credentials, `.env` files, or password hashes with real values
- return stack traces, SQL, provider errors, secrets, or internal paths to production clients
- expose private/original photos through public DTOs
- make a UUID alone the authorization rule for private content

Admin authentication uses:

```text
Spring Security
Server-side session
Secure HttpOnly host-only cookie
CSRF token
Exact CORS allowlist
BCrypt admin credential
```

Security changes require focused tests for authentication, authorization, CSRF, CORS, cookie attributes, visibility, and safe errors.

## 11. Cloudinary and Upload Rules

The default upload flow is:

```text
Admin Browser
    ├── requests signed parameters from Spring Boot
    └── uploads image bytes directly to Cloudinary

Spring Boot
    ├── authenticates and authorizes the admin
    ├── signs fixed upload parameters
    ├── verifies the completed Cloudinary asset
    └── persists trusted metadata
```

Do not route multi-megabyte image bytes through Spring Boot as the default flow.

- Originals use Cloudinary `type=private`
- Strict Transformations must be enabled
- Public delivery is limited to fixed `thumbnail`, `card`, and `display` variants
- The browser cannot choose arbitrary transformation strings, folders, delivery types, or public IDs
- Upload signatures bind a generated public ID, fixed folder, private type, preset, timestamp, no-overwrite policy, and fixed variants
- Public derivatives strip metadata; GPS/raw EXIF is never copied wholesale

## 12. Testing Rules

Run the tests relevant to every completed feature.

Frontend checks, when available:

```text
typecheck
lint
unit tests where appropriate
Playwright for important user flows
```

Backend checks:

```text
./mvnw test
```

Backend priority scenarios:

- visibility filtering and private 404 behavior
- archive ordering and date filters
- validation and database constraints
- admin authentication/authorization
- CSRF, CORS, secure cookies, and safe errors
- Cloudinary signature and asset verification

Never report "Tests passed" unless the command was actually executed and succeeded in this task. If a relevant test cannot run, report `Not run` and the concrete reason. Documentation-only validation is not application test execution.

## 13. Documentation Update Rules

Update documentation only when a change affects:

- product behavior or user flow
- architecture or dependency choices
- API contracts
- persisted data model or migrations
- security behavior
- development workflow

Do not update every design document for a small CSS or implementation-only change. Avoid documentation churn that adds no durable information.

Step 1–3 documents are frozen unless the user explicitly asks to revise the relevant design. When an approved implementation decision differs from a frozen document, update only the affected source-of-truth document and record the reason.

## 14. Git and Worktree Safety

- Inspect `git status` before editing
- Preserve unrelated user changes and untracked work
- Never commit `.env`, API secrets, production photos, `node_modules/`, `.next/`, `target/`, coverage, or logs
- Do not delete `prototype/` or its tracked temporary test-photo dependencies
- Do not use destructive commands such as `git reset --hard` or broad recursive deletion
- Do not force-push, rewrite history, amend unrelated commits, or delete unrelated files unless explicitly requested
- Do not commit or push unless the user explicitly asks
- Keep commits focused on one feature when commits are requested
- Review the final diff and changed-file list before reporting completion

## 15. Agent Responsibility Split

- Frontend-focused tasks are primarily handled by the Frontend Agent
- Backend, database, media, and security tasks are primarily handled by the Backend Agent
- Cross-boundary changes require both contracts to remain aligned

These labels describe responsibility, not a specific AI vendor or product. Any agent working in the repository must follow this file.

## 16. Completion Report

Every completed task report must include:

```text
Changed files
What changed
Tests executed
Test results
Docs updated
Known limitations / follow-up
```

Use `None for this scope` when there are no known limitations. Distinguish application tests from static/document checks, and explicitly say `Not run` when no tests were executed.
