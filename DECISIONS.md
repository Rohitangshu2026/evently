# Design decisions

Why the non-obvious choices in this codebase were made, and what was rejected.

## Pessimistic locking on the purchase path

Selling a ticket is check-then-insert: count what's sold, compare against
capacity, insert. Two transactions running that concurrently can both pass the
check and oversell. The tier row is read `FOR UPDATE`, so competing purchases
serialize on one row while other tiers sell in parallel.

Rejected alternatives: **optimistic locking with retries** works but degrades
badly exactly when it matters — a drop-style rush turns into a retry storm
where most attempts burn a round trip only to fail version checks.
**`SERIALIZABLE` isolation** pushes the same problem to commit-time aborts and
also needs retry loops. **A Redis lock** adds an infrastructure dependency and
a second source of truth for a problem the database already solves; it earns
its place only when inventory checks must happen without touching the primary
database at all. The row lock is the smallest tool that makes the invariant
unconditional; the k6 run in `backend/loadtest/` (200 buyers, 50 seats,
exactly 50 sold) is the evidence.

## Idempotency keys on purchase

A client that times out and retries must not buy two tickets. The purchase
endpoint accepts an `Idempotency-Key`; the (user, key) pair maps to the
resulting ticket and replays return the original. The mapping lives in a table
with a unique constraint, so even two same-key requests racing each other
can't both create tickets — one hits the constraint and its retry lands on the
replay path. Replays are resolved before any locking so retries never queue on
the hot row.

## Ed25519 (EdDSA) JWT signatures

HS256 was rejected because it's symmetric: anything that can verify a token
can mint one, which turns every future service that validates tokens into a
key-compromise risk. Between the asymmetric options, Ed25519 over RS256:
smaller keys and signatures, faster signing, no RSA padding pitfalls, and
deterministic signatures. Verification needs only the public key and no
database access, which keeps request authentication stateless. Production
rotation would add a `kid` header and a JWKS endpoint; a single committed
dev-only keypair (clearly marked, see `backend/src/main/resources/keys/`)
keeps the demo runnable out of the box.

## Refresh-token rotation with family revocation

Access tokens live 15 minutes, so the refresh token is the real session
credential and the thing worth stealing. Every refresh rotates it: the
presented token is revoked and a new one is issued in the same *family*.
Presenting an already-revoked token is the signature of a replayed stolen
token, and the response is to revoke the entire family — attacker and victim
both lose the session, the victim just logs in again. Only the SHA-256 hash of
a token is stored, so a database leak alone can't forge sessions. One
implementation detail worth knowing: the family revocation runs in a
`REQUIRES_NEW` transaction, because the request that triggers it ends in a 401
and a rollback would otherwise silently undo the revocation. An integration
test pins exactly that scenario.

## Refresh token in an httpOnly cookie, access token in memory

localStorage is readable by any script that gets to run on the page, so an XSS
bug there hands out long-lived sessions. The refresh token travels only in an
`httpOnly; SameSite=Strict` cookie path-scoped to `/api/v1/auth` — JavaScript
can't read it, and it isn't attached to cross-site requests, which also closes
CSRF against the auth endpoints. The short-lived access token stays in SPA
memory where an XSS payload's window of use is minutes, not weeks.

## Argon2id for password hashing

bcrypt is fine; Argon2id is better against the actual threat, which is
offline cracking on GPUs — it's memory-hard, so the attacker's parallelism is
bounded by memory bandwidth rather than compute. It's also OWASP's first
recommendation. Login compares against a dummy hash when the email doesn't
exist, so response timing doesn't reveal which addresses have accounts.

## Flyway migrations, Hibernate in validate mode

`ddl-auto=update` lets the ORM mutate production schemas as a side effect of
refactoring, with no review and no rollback story. Flyway makes every schema
change a versioned, reviewable SQL file, and `validate` mode turns any drift
between entities and schema into a startup failure instead of a runtime
surprise.

## BigDecimal for money

`Double` cannot represent most decimal fractions exactly; accumulate enough
49.99s and the books stop balancing. Prices are `BigDecimal` in Java and
`numeric(10,2)` in Postgres end to end.

## QR codes as opaque bearer credentials

The QR image encodes 256 bits of randomness, not ticket data. The scanner
sends the value back and the server resolves it, so nothing about the event,
seat or holder is readable from a screenshot, and forging a credential means
guessing a 256-bit value (a unique constraint guarantees no collisions).
Validation consumes the credential under the same `FOR UPDATE` discipline as
purchase, because two gates scanning one ticket simultaneously is the same
lost-update race.

## Real Postgres in integration tests, never H2

The things these tests exist to prove — row locking, transaction isolation,
constraint behaviour — are exactly the things H2 emulates loosely or not at
all. A green H2 suite says little about production behaviour. Tests run
against a disposable Postgres database; Testcontainers is the intended
mechanism (wiring kept in `AbstractIntegrationTest`), with a fixed test
database as the local fallback where the Docker API version outruns the
bundled client.

## In-memory rate limiting on auth endpoints

Login and signup are throttled per IP with token buckets (Bucket4j). In-memory
state is a deliberate single-instance simplification: the moment this runs on
two nodes the buckets must move to a shared store (Redis), and the filter is
isolated so that swap touches one class.

## Known simplification: self-service roles

Signup lets a user register as organizer, attendee or staff. Real systems
invite staff and verify organizers; collapsing that flow keeps the demo
self-serve. The role model itself (roles as authorities, method-level guards)
wouldn't change — only how roles are granted.
