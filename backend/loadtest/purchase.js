/**
 * Oversell proof for the ticket purchase path.
 *
 * Scenario: 200 virtual users race to buy from a tier with exactly 50 seats.
 * The run FAILS unless precisely 50 requests succeed (201) and the remaining
 * 150 are rejected as sold out (409) — i.e. the pessimistic lock holds under
 * real HTTP concurrency, not just in unit conditions.
 *
 * The script seeds its own data (organizer, published event, one attendee) so
 * it can run against a fresh backend:
 *
 *   docker compose up -d && mvn spring-boot:run   # in backend/
 *   k6 run loadtest/purchase.js
 *
 * Note: buyers share one attendee account on purpose — the signup endpoint is
 * rate-limited (5/min/IP), and the oversell invariant is per-tier, not
 * per-buyer, so one hot account exercises it just as well.
 */
import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const SEATS = 50;
const BUYERS = 200;

const created = new Counter('purchase_created');
const soldOut = new Counter('purchase_sold_out');
const unexpected = new Counter('purchase_unexpected');

export const options = {
    scenarios: {
        rush: {
            executor: 'shared-iterations',
            vus: BUYERS,
            iterations: BUYERS,
            maxDuration: '2m',
        },
    },
    thresholds: {
        // The invariant, enforced as pass/fail: exactly SEATS sales, the rest
        // rejected cleanly, nothing else.
        purchase_created: [`count==${SEATS}`],
        purchase_sold_out: [`count==${BUYERS - SEATS}`],
        purchase_unexpected: ['count==0'],
    },
};

/** Signs up a user and returns their access token. */
function signup(role, marker){
    const res = http.post(`${BASE}/api/v1/auth/signup`, JSON.stringify({
        email: `${role.toLowerCase()}-${marker}@loadtest.evently`,
        password: 'password123',
        name: `${role} LoadTest`,
        role: role,
    }), { headers: { 'Content-Type': 'application/json' } });
    if(res.status !== 201){
        fail(`signup ${role} failed: ${res.status} ${res.body}`);
    }
    return res.json('accessToken');
}

/**
 * Seeds the race: one organizer, one published event with a SEATS-seat tier,
 * one attendee to do the buying. Runs once before the scenario starts.
 */
export function setup(){
    const marker = Date.now();
    const organizerToken = signup('ORGANIZER', marker);

    const eventRes = http.post(`${BASE}/api/v1/events`, JSON.stringify({
        name: `Oversell Proof ${marker}`,
        venue: 'Load Test Arena',
        status: 'PUBLISHED',
        ticketTypes: [{
            name: 'General',
            price: 25.00,
            description: 'Load test seat',
            totalAvailable: SEATS,
        }],
    }), {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${organizerToken}`,
        },
    });
    if(eventRes.status !== 201){
        fail(`event creation failed: ${eventRes.status} ${eventRes.body}`);
    }

    return {
        attendeeToken: signup('ATTENDEE', marker),
        eventId: eventRes.json('id'),
        tierId: eventRes.json('ticketTypes.0.id'),
    };
}

/** One buyer: a single purchase attempt, classified by outcome. */
export default function(data){
    const res = http.post(
        `${BASE}/api/v1/events/${data.eventId}/ticket-types/${data.tierId}/tickets`,
        null,
        { headers: { Authorization: `Bearer ${data.attendeeToken}` } },
    );

    if(res.status === 201){
        created.add(1);
    } else if(res.status === 409){
        soldOut.add(1);
    } else {
        unexpected.add(1);
    }

    check(res, { 'sold or cleanly rejected': (r) => r.status === 201 || r.status === 409 });
}
