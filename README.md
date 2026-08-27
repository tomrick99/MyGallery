# MyGallery

MyGallery is a personal photography website organized around time.

It is not a conventional gallery grid or a blog. It is a continuously evolving **Personal Photographic Archive** built around one principle:

> Photography First.

Visual direction:

```text
Modern Editorial Photography
× Apple Minimalism
× Analog Film Feeling
```

## Product Experience

```text
Homepage
├── Random Featured Hero
├── Horizontal Selected Frames
└── Timeline Archive

About

Photo
└── Fullscreen Lightbox
```

The primary content structure is time:

```text
Year
└── Month
    └── Photos
```

Tags and categories may become supporting metadata later, but they are not the primary navigation or V1 content model.

## Architecture

```text
Visitor
   │
   ▼
Next.js / Vercel
   │
 REST / HTTPS
   ▼
Spring Boot / Railway
   │
   ├── PostgreSQL / Neon
   └── Cloudinary
```

| Part | Responsibility |
|---|---|
| Frontend | Presentation, editorial layout, and browser interactions |
| Backend | Business authority, visibility, validation, authentication, and media policy |
| PostgreSQL | Photo metadata only |
| Cloudinary | Private originals and fixed web-sized image variants |

The frontend never accesses PostgreSQL directly. Image binaries do not belong in PostgreSQL, and production original photographs must never be committed to GitHub.

## Technology Stack

| Area | Technology |
|---|---|
| Frontend | Next.js 16, React, TypeScript, App Router, CSS Modules |
| Backend | Java 21 LTS, Spring Boot 4.1.x, Spring Web, Spring Security, Spring Data JPA, Bean Validation, Flyway |
| Database | PostgreSQL on Neon |
| Media | Cloudinary |
| Hosting | Vercel for frontend, Railway for backend |
| Source | GitHub monorepo |

The architecture is a separated frontend/backend monorepo with a Spring Boot modular monolith and REST/JSON API under `/api/v1/*`.

## Repository Structure

Target structure:

```text
MyGallery/
├── frontend/        # Next.js application — Step 5+
├── backend/         # Spring Boot application — Step 5+
├── docs/            # Product and technical design
├── prototype/       # Step 1 visual direction reference
├── testphoto/       # Temporary prototype test assets
├── README.md
├── AGENTS.md
└── .gitignore
```

`prototype/` is a **Visual Direction Reference**, not the production frontend. Formal development belongs in `frontend/`; do not copy the prototype's Vanilla JS implementation directly into Next.js.

`testphoto/` temporarily supports the prototype. It is not a long-term store for production photography.

## Documentation

| Document | Purpose |
|---|---|
| [Product Requirements](docs/PRD.md) | Product goals, scope, information architecture, and Photography First principles |
| [User Flow](docs/USER_FLOW.md) | Visitor journeys, archive navigation, lightbox behavior, and future admin flow |
| [Frontend Technical Design](docs/FRONTEND_TECHNICAL_DESIGN.md) | Next.js architecture, Server/Client boundaries, image variants, layout, and performance |
| [Backend Technical Design](docs/TECHNICAL_DESIGN.md) | Spring Boot modular monolith, REST API, media flow, database, deployment, and testing |
| [Data Model](docs/DATA_MODEL.md) | Photo fields, constraints, derived values, indexes, DTOs, and archive response |
| [Security Design](docs/SECURITY.md) | Admin authentication, cookies, CSRF, CORS, secrets, uploads, and original protection |

These documents are the source of truth for implementation. Agent-specific working rules are in [AGENTS.md](AGENTS.md).

## Current Development Status

```text
Step 1 — Visual Direction       ✅
Step 2 — Product Definition     ✅
Step 3 — Technical Design       ✅
Step 4 — Repository Rules       🚧 complete in working tree; mark ✅ after commit
Step 5 — Implementation         ⏳
```

The production frontend and backend have not been created yet. The repository currently contains the frozen product/technical design and the visual prototype.

## Development Workflow

```text
Read relevant docs
        ↓
Choose one feature
        ↓
Implement locally
        ↓
Run relevant tests
        ↓
Review the change
        ↓
Update affected docs if needed
        ↓
Commit
        ↓
Next feature
```

Build one coherent feature at a time. Do not ask an AI coding agent to generate the entire website in one uncontrolled change.

## Security Philosophy

```text
Original photographs are private.

The public site receives only web-sized derivatives.

No website can prevent screenshots or saving
an already rendered image.
```

The practical goals are to protect originals, control public resolution, protect the admin surface, and increase scraping cost. See [Security Design](docs/SECURITY.md) for the full threat model and controls.

## Deployment Plan

```text
frontend/  → Vercel
backend/   → Railway
PostgreSQL → Neon
Photos     → Cloudinary
```

This is the planned production topology only. Step 4 does not create services, configure providers, or deploy the application.
