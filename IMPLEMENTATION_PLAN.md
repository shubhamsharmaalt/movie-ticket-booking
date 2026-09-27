# Movie Ticket Booking System — Implementation Plan

## Goal and boundary

Build a local, explainable Spring Boot REST API for the complete movie-ticket flow in the supplied brief. Use PostgreSQL so the same JDBC configuration can point to a Supabase database later. There is no frontend, deployment setup, CI/CD, microservice split, or external payment provider. Do not initialize Git or make commits; the project owner will handle the repository and commit history.

This document is the design and execution plan. Application code starts only after the owner reviews it.

## Approach

| Option | Trade-off | Decision |
| --- | --- | --- |
| Spring Data JPA with PostgreSQL row locks | Small amount of explicit transactional code; good fit for the brief and easy to explain | **Use** |
| Handwritten JDBC for every query | More precise SQL but much more persistence code | Do not use |
| In-memory seat locks | Simple locally but cannot reliably prevent double allocation once more than one process accesses the database | Do not use |

One Spring Boot application owns the API, business rules, scheduled jobs, and database access. Seat allocation is serialized in PostgreSQL transactions, not in Java process memory. Spring Security HTTP Basic supplies demo admin/customer identities; this is intentionally basic authentication, as requested by the brief.

**Stack:** Java 21, Maven, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Flyway, PostgreSQL JDBC driver. H2 is used only by automated tests. These dependencies will be added when implementation begins. Java 21 and Maven are already available on this machine.

## Explicit product assumptions

1. A customer pays through a **local payment simulator**. It records successful or failed attempts and a generated reference; it never accepts card details or moves money. A successful attempt confirms the held booking. A refund is a recorded simulated refund. No provider was specified in the brief.
2. Notifications are delivered to an **in-app inbox exposed by REST**. A background job processes database-backed pending notifications, so confirmation and reminder delivery does not delay booking responses. No SMTP, SMS, or push provider is required for the local deliverable.
3. Basic Auth has one admin and two customer demo accounts, configured from local environment variables and documented in the README. Booking ownership uses the authenticated username. There is no signup, OAuth, or account-management API.
4. A hold lasts five minutes by default. An expired hold cannot be paid; its seats become available even if the cleanup job has not run yet. The cleanup job makes the expired state visible in booking history and releases stale rows.
5. Seats have `REGULAR` or `PREMIUM` tiers. Every show has a price for each tier plus a configurable weekend surcharge percentage. Weekend is determined in the city's IANA time zone. The price and discount are frozen on the hold, so later admin edits cannot change an existing booking.
6. A percentage discount code is optional, active only within its configured validity window. One code applies to an entire booking. No stacking or usage quota is planned.
7. A show's assigned refund policy defines a full-refund cutoff, a partial-refund cutoff, and a partial-refund percentage. Validate that the full cutoff is no shorter than the partial cutoff. The policy values are frozen at confirmation. Cancellation is allowed before the show begins; at or after start it is rejected. Cancellation after the partial cutoff but before start gives zero refund.
8. A theater's seat layout is editable until a show is created for it. That prevents an admin edit from invalidating existing show-seat or booking records.
9. Money is stored as fixed-precision decimal values; times are stored as UTC instants. API timestamps use ISO 8601. API lists use basic pagination.

## Data model and invariants

| Table | Purpose / key constraints |
| --- | --- |
| `cities` | City name and IANA time zone; unique name. |
| `theaters` | Theater belongs to one city; unique name within a city. |
| `theater_seats` | Seat label and `REGULAR`/`PREMIUM` tier; unique label within a theater. |
| `movies` | Title and duration. |
| `refund_policies` | Named cutoff values and partial percentage. |
| `discount_codes` | Unique code, percent, validity window, active flag. |
| `shows` | Movie, theater, UTC start, show prices, weekend surcharge, refund policy; reject overlapping shows in the same theater. |
| `show_seats` | One row per seat per show; unique `(show_id, theater_seat_id)`; `AVAILABLE`, `HELD`, or `BOOKED`, with current booking reference and hold expiry. This is the allocation authority. |
| `bookings` | Customer, show, `HELD`/`CONFIRMED`/`EXPIRED`/`CANCELLED`, expiry, calculated total, discount and refund-policy snapshots. |
| `booking_seats` | Booked seat labels and price snapshots retained for history even after cancellation. |
| `payments` | Attempt result, amount, generated reference; only one successful payment per booking. |
| `refunds` | One cancellation refund record per confirmed booking, including zero-refund cancellations. |
| `notifications` | Customer, booking, `CONFIRMATION`/`REMINDER`/`CANCELLATION`, pending/delivered state; unique `(booking_id, type)`. |

Flyway owns the schema; Hibernate validates it and never auto-creates or silently changes tables. Foreign keys, uniqueness, and indexes cover the relationships and the main show, booking-history, expiry, and notification queries.

## REST API

All paths use `/api`. Return standard JSON responses and a consistent validation/error body. `400` means invalid input, `401` unauthenticated, `403` wrong role or ownership, `404` missing resource, and `409` a booking/seat conflict.

| Role | Endpoints | Behavior |
| --- | --- | --- |
| Public browse | `GET /cities`, `/movies`, `/shows?cityId=&date=&page=`, `/shows/{id}/seats` | Browse catalog and live effective availability (expired holds appear free). |
| Admin | `GET/POST/PUT/DELETE /admin/cities`, `/admin/theaters`, `/admin/movies`, `/admin/refund-policies`, `/admin/discount-codes` | List/detail/create/update; delete only when no dependent records exist, otherwise `409`. |
| Admin | `GET/POST/PUT/DELETE /admin/theaters/{id}/seats` | Maintain layout only before the theater has shows. |
| Admin | `GET/POST/PUT/DELETE /admin/shows` | List/detail/create/update show and pricing; delete only if there are no bookings. Creating a show snapshots the theater's seats into `show_seats`. |
| Customer | `POST /bookings/holds` | Supply show ID, distinct seat IDs, optional discount code; return booking ID, price breakdown, expiry. |
| Customer | `POST /bookings/{id}/payments` | Simulate `SUCCESS` or `FAILURE`; success confirms an unexpired owned hold. Repeating a successful request returns the confirmed booking without a second charge. |
| Customer | `POST /bookings/{id}/cancellation` | Cancel an owned confirmed booking before show start; return computed refund. Repeating returns the existing cancellation. |
| Customer | `GET /bookings?page=`, `GET /bookings/{id}`, `GET /notifications?page=` | Only the signed-in customer's records. |

Admin updates that would rewrite a paid booking's meaning are rejected. The README will contain exact request/response examples and a short `curl` walkthrough from catalog setup to refund.

## Transaction flows

**Hold:** Validate show timing and seats. In one database transaction, lock requested `show_seats` rows in a consistent seat-ID order with `PESSIMISTIC_WRITE`. Reject any booked seat or active hold. Treat an expired hold as free. Calculate and snapshot prices, create the `HELD` booking and its seat snapshots, then mark the locked rows held with the booking ID and expiry. Two competing requests for the same seat cannot both commit successfully.

**Payment:** Lock the booking, verify customer ownership, `HELD` status, expiry, and that every `show_seat` still refers to it. A simulated failure is recorded without confirming. On success, record one successful payment, mark the booking `CONFIRMED`, mark its seats `BOOKED`, and enqueue a confirmation notification in the same transaction.

**Expiry:** The scheduled cleanup finds due `HELD` bookings and uses the same booking-then-seat lock order as payment. It marks them `EXPIRED` and releases only seats that still point at that booking. The hold and payment paths enforce expiry directly, so scheduler delay never extends a hold.

**Cancellation:** Lock the confirmed booking and seats, check show start, calculate the frozen-policy refund, record one refund, mark `CANCELLED`, release its seats, and enqueue a cancellation notification in one transaction.

**Notifications:** A scheduled job creates one reminder for each confirmed booking approaching show time. Another scheduled pass changes pending inbox messages to delivered; failures remain pending for retry. No external network call occurs in the booking transaction. The customer's notification endpoint returns delivered messages.

## Implementation sequence

Each step ends with a working, testable slice. No commits are part of this plan.

1. **Project skeleton and configuration.** Add `pom.xml`, application entry point, `application.yml`, `.env.example`, `.gitignore`, and test profile. Main profile reads `DB_URL` (for example `jdbc:postgresql://HOST:5432/postgres?sslmode=require`), `DB_USERNAME`, and `DB_PASSWORD`; tests use H2. Supabase's project API URL is not a JDBC URL. Confirm the app fails clearly when DB configuration is missing.
2. **Schema and catalog.** Add the first Flyway migration, entities/repositories, admin catalog APIs, public browse APIs, validation, and consistent exception responses. Verify city → theater → seat → movie → show creation and show-seat materialization. Reject conflicting show schedules and unsafe admin edits.
3. **Roles and ownership.** Configure HTTP Basic and role rules. Verify public browse, admin-only mutations, customer-only bookings, and cross-customer denial.
4. **Pricing and holds.** Implement weekday/weekend tier pricing, discount validation, hold creation, availability, and expiry cleanup. Unit-test price math and time boundaries; integration-test multi-seat atomicity and competing holds.
5. **Payment and confirmation.** Add simulated payment attempts, confirmation transition, booking history, and idempotent repeated success. Test failed, expired, duplicate, and wrong-owner cases.
6. **Cancellation and refunds.** Add policy snapshots, cancellation transition, refund records, seat release, and policy boundary tests.
7. **Notifications.** Add pending/delivered inbox records, confirmation and reminder jobs, retry behavior, and customer inbox API. Verify they do not hold up the booking response.
8. **Documentation and handoff.** Add `README.md`, root `AGENTS.md`, `SKILLS_USED.md`, and a copy of the supplied PDF in `docs/source/`. Explain every assumption above, database setup, environment variables, API examples, test commands, concurrency design, limitations, and points to cover in the owner's video.
9. **Final verification.** Run `mvn test`, run the application against a configured PostgreSQL database, execute the documented `curl` flow, and run a two-customer same-seat race. If Supabase credentials are not available, report the PostgreSQL smoke test as unverified rather than claiming it passed.

## Required test coverage

- **Unit:** price tier + weekend time zone, discount validity/rounding, hold deadline, refund cutoff and percentage.
- **Integration:** admin/customer access, catalog setup, hold → successful payment → booking history, payment failure, expiry/rehold, cancellation/refund/rebook, ownership isolation, notification creation/reminder delivery, and two simultaneous attempts for one seat.
- **Database:** H2 runs the ordinary automated integration suite without credentials. A PostgreSQL-specific integration run is available through environment variables to verify actual row-lock behavior against a disposable PostgreSQL database or test schema before submission.

## Requirement check

| Brief requirement | Planned coverage |
| --- | --- |
| Multiple cities, theaters, shows, seat layouts | Catalog model and admin/public APIs |
| Time-bound holds and automatic expiry | Hold transaction, effective availability, cleanup job |
| Regular, premium, weekend, discounts | Seat tier + show pricing + code calculation |
| Payment, confirmation, refunds | Simulated payment/refund records and booking state transitions |
| Concurrent seat booking without double allocation | Database row locks, unique show-seat rows, race test |
| Non-blocking confirmation/reminder notifications | Database-backed inbox and background jobs |
| Admin/customer roles | Spring Security HTTP Basic and ownership checks |
| Validation, errors, unit/integration tests | API validation, error schema, test suites above |
| README, agent instructions, skills, raw files | Handoff step above |

The GitHub URL, commit history, and video recording remain with the project owner, as explicitly requested.

## Technical references

- [Spring Boot 4.1.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Supabase PostgreSQL connection modes and JDBC guidance](https://supabase.com/docs/guides/database/connecting-to-postgres)
