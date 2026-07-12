# Oversell load test — results

**Claim under test:** with a 50-seat ticket tier and 200 concurrent buyers, the
purchase path sells *exactly* 50 tickets and cleanly rejects the other 150 —
the pessimistic row lock prevents oversell under real HTTP concurrency.

## Run

- Date: 2026-07-12
- Command: `k6 run loadtest/purchase.js` (k6 v1.3.x)
- Backend: local `mvn spring-boot:run`, Java 17, PostgreSQL 16 (Docker),
  Hikari defaults (10-connection pool)
- Scenario: `shared-iterations`, **200 VUs × 200 iterations**, one purchase per
  iteration, single shared attendee account, no idempotency keys

## Outcome — all thresholds passed

| Threshold | Expected | Actual |
|---|---|---|
| `purchase_created`    | count == 50  | **50** ✓ |
| `purchase_sold_out`   | count == 150 | **150** ✓ |
| `purchase_unexpected` | count == 0   | **0** ✓ |

Cross-checked server-side after the run via `/actuator/prometheus`:

```
evently_tickets_purchased_total        50.0
evently_purchase_latency_seconds_count 200
```

## Latency under contention

All 200 buyers slam a single tier row, so every request serializes on one
`SELECT ... FOR UPDATE` lock — this is the worst case by design:

| Metric (http_req_duration) | Value |
|---|---|
| median | 322.65 ms |
| p90    | 402.68 ms |
| p95    | 412.52 ms |
| max    | 422.84 ms |
| throughput | ~230 req/s, all 200 iterations completed in 0.9 s |

Reading the numbers: requests queue on the row lock, so latency grows roughly
linearly with queue position — the max (~423 ms) is the 200th buyer in line.
Different tiers lock different rows and would sell in parallel; the queue is
per-tier, which is exactly the granularity the invariant needs.

Note: k6 reports `http_req_failed 73.89%` for this run — that is the 150
deliberate 409 "sold out" responses, which are the *correct* outcome for
losing buyers, not errors.

## Reproduce

```bash
docker compose up -d                 # repo root: Postgres 16
cd backend && mvn spring-boot:run    # JDK 17
k6 run loadtest/purchase.js          # exits non-zero if any threshold fails
```

The script seeds its own organizer, published event and attendee, so it runs
against a clean database with no manual setup.
