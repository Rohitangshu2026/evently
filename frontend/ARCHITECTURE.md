# Evently Frontend

This is the web client for Evently, an event ticketing platform. It's a single-page React app that talks to a Spring Boot backend over a REST API and uses Keycloak for authentication.

The app has to serve three pretty different audiences, and most of the structure here follows from that split:

- **Organizers** create and manage events, set up ticket types, and watch their sales.
- **Attendees** browse what's on, buy tickets, and pull up the QR code for an event when they show up.
- **Staff** scan those QR codes at the door to let people in.

## Stack

- **React 19** with **TypeScript**, bundled by **Vite 6** (using the SWC plugin for fast refresh).
- **Tailwind CSS 4** for styling. The visual language is intentionally a bit editorial — serif display type (Fraunces), lots of whitespace, gold accents.
- **Radix UI** primitives wrapped in shadcn-style components under `src/components/ui`. These are the buttons, dialogs, dropdowns, selects, calendars, etc. — unstyled-but-accessible primitives with our Tailwind classes on top.
- **React Router 7** for client-side routing (`createBrowserRouter`).
- **react-oidc-context** + **oidc-client-ts** for the OpenID Connect flow against Keycloak.
- **@yudiel/react-qr-scanner** for the staff scanning screen.
- **date-fns** and **react-day-picker** for date handling and the event date pickers.
- **lucide-react** for icons.
- **json-server** as a stand-in API during development (see below).

There's a `@/` path alias pointing at `src/`, so imports look like `@/lib/api` rather than a trail of `../../`.

## Project structure

```
src/
  pages/        one component per route (landing pages, dashboard screens, etc.)
  components/   shared building blocks
    ui/         the Radix/shadcn primitives
  hooks/        custom hooks (currently just role extraction)
  lib/          the API client and small utilities
  domain/       TypeScript types that mirror the backend's DTOs and enums
  assets/       static assets
```

## Routing

All routes are declared in `src/main.tsx`. Anything that needs a logged-in user is wrapped in `ProtectedRoute`, which bounces you to the login flow if there's no session.

Public:

- `/` — attendee landing page
- `/organizers` — organizer landing page
- `/events/:id` — public event detail; this is where an attendee picks a ticket type
- `/login` and `/callback` — the login screen and the OIDC redirect target

Protected:

- `/events/:eventId/purchase/:ticketTypeId` — purchase flow for a specific ticket type
- `/dashboard` — dashboard home
- `/dashboard/events` — organizer's list of their events
- `/dashboard/events/create` and `/dashboard/events/update/:id` — the same `DashboardManageEventPage` component handles both create and edit, keyed off whether there's an `:id`
- `/dashboard/tickets` and `/dashboard/tickets/:id` — an attendee's purchased tickets, and a single ticket with its QR code
- `/dashboard/validate-qr` — the staff scanning screen

## Auth and roles

Authentication is delegated to Keycloak. The OIDC config lives at the bottom of `main.tsx`:

```ts
const oidcConfig = {
  authority: "http://localhost:9090/realms/event-ticket-platform",
  client_id: "event-ticket-platform-app",
  redirect_uri: "http://localhost:5173/callback",
};
```

So for the app to actually log anyone in, you need Keycloak running on port 9090 with a realm named `event-ticket-platform` and a client called `event-ticket-platform-app`. The `AuthProvider` from `react-oidc-context` wraps the whole router, which is what makes `useAuth()` available everywhere.

Authorization is role-based. The access token Keycloak issues carries the user's roles under the `realm_access.roles` claim. `hooks/use-roles.tsx` decodes the JWT, keeps the roles that start with `ROLE_`, and hands back convenient booleans:

```ts
const { isOrganizer, isAttendee, isStaff } = useRoles();
```

The three roles the UI cares about are `ROLE_ORGANIZER`, `ROLE_ATTENDEE`, and `ROLE_STAFF`. Navigation and dashboard options branch on these — the public navbar (`public-nav.tsx`) and the authenticated one (`nav-bar.tsx`) are separate components for that reason.

## Talking to the backend

Everything network-related goes through `src/lib/api.ts`. It's a thin set of typed wrappers around `fetch`, all hitting `/api/v1/...`. A few conventions worth knowing:

- `handle<T>()` is the single place that reads a response. It deals with empty bodies, non-JSON error pages, and JSON, and always throws a clean `Error` on a non-2xx so callers can just `try/catch`.
- `friendlyStatus()` turns raw HTTP status codes into human messages, so a 403 becomes "You don't have permission to perform that action" rather than a bare number.
- `authHeaders(accessToken)` attaches the bearer token; calls that don't need auth use plain JSON headers. The access token is pulled from the OIDC session at the call site and passed in.
- List endpoints return Spring's pagination envelope, typed here as `SpringBootPagination<T>` — it carries `content`, `totalPages`, `totalElements`, and the page metadata, which `simple-pagination.tsx` renders.
- The ticket QR code comes back as an image, so `getTicketQr()` returns a `Blob` instead of JSON.

The request/response shapes live in `src/domain/domain.ts` — `CreateEventRequest`, `EventDetails`, `TicketSummary`, the status enums, and so on. Keeping them in one file makes it easy to keep the frontend honest against the backend contract.

## Running it locally

```bash
npm install
npm run dev        # Vite dev server on http://localhost:5173
```

The dev server expects two things alongside it:

1. The Spring Boot backend answering on `/api/v1` (proxy the dev server to it, or run them same-origin).
2. Keycloak on `:9090` with the realm and client above, otherwise login won't complete.

If you just want to poke at the UI without a real backend, there's a mock:

```bash
npm run mocks      # json-server backed by db.json
```

Other scripts:

```bash
npm run build      # type-check (tsc -b) then production build
npm run lint       # eslint
npm run format     # prettier
```

## Status

The frontend is the more complete half of the project right now. The Spring Boot backend that serves `/api/v1` is being built out separately; until it's wired up end-to-end, `json-server` and `db.json` are enough to develop most screens against.
