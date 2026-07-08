# ⚠️ DEV-ONLY signing keys — DO NOT use in production

The Ed25519 keypair in this directory (`jwt_ed25519_private.pem` /
`jwt_ed25519_public.pem`) exists **only** so the project runs out of the box for
local development and review. It is committed deliberately and must be treated
as **public knowledge**: anyone with this repo can mint tokens this instance
would accept.

In any real deployment:

1. Generate a fresh keypair (never reuse this one):
   ```bash
   openssl genpkey -algorithm ed25519 -out jwt_ed25519_private.pem
   openssl pkey -in jwt_ed25519_private.pem -pubout -out jwt_ed25519_public.pem
   ```
2. Supply it via a secret manager or mounted volume, and point the app at it
   with the `app.jwt.private-key-location` / `app.jwt.public-key-location`
   properties (they accept any Spring resource location, e.g. `file:/...`).
3. Rotate keys periodically. The production-grade evolution is a `kid` header
   on issued tokens plus a JWKS endpoint, letting verifiers hold multiple
   public keys during rotation windows.
