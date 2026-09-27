# Movie Ticket Booking System

A local Spring Boot REST API for the SDE-2 take-home brief. It supports multiple cities, theaters and shows, seat-level booking, five-minute holds, tier and weekend pricing, discounts, simulated payment and refunds, customer/admin roles, and a notification inbox. There is no frontend.

[Watch the 4:50 code walkthrough](docs/walkthrough/movie-ticket-booking-walkthrough.mp4), or read its [transcript](docs/walkthrough/transcript.md).

The [implementation plan](IMPLEMENTATION_PLAN.md) records the design decisions. The supplied brief is preserved at [docs/source/Movie Ticket Booking System.pdf](docs/source/Movie%20Ticket%20Booking%20System.pdf). [AGENTS.md](AGENTS.md) contains the development instructions used for this project, and [SKILLS_USED.md](SKILLS_USED.md) records the skills used.

## Stack and why

| Tool | Reason |
| --- | --- |
| Java 21 + Spring Boot 4.1.1 | The brief requires Spring Boot; Java 21 is an LTS version. |
| Spring Web MVC | Small, conventional REST controllers. |
| Spring Data JPA | Repository access and transactional row locks without handwritten SQL for every operation. |
| PostgreSQL + Flyway | Durable data, schema versioning, and database-level serialization of seat allocation. Supabase provides PostgreSQL. |
| Spring Security HTTP Basic | Demonstrates the required admin/customer access rules without an OAuth system. |
| JUnit + Spring Boot tests + H2 | Unit and API/database integration tests run locally without Supabase credentials. |

## Code layout

The Java packages separate HTTP handling from business rules and persistence:

| Package | Responsibility |
| --- | --- |
| `controller` | REST endpoints and HTTP responses |
| `dto` | Validated request and response records |
| `service` | Catalog, booking, and notification rules and transactions |
| `entity` | JPA entities and domain enums |
| `repository` | Spring Data persistence and locking queries |
| `jobs` | Hold expiry and notification schedules |
| `config`, `error`, `util` | Basic Auth, API errors, and pure pricing/refund calculations |

Tests are grouped under `integration` and `util` to match their purpose.

## Run locally with PostgreSQL or Supabase

You need Java 21, Maven 3.6.3 or later, and a PostgreSQL database. This repository does **not** contain credentials.

1. Copy `.env.example` to `.env` and replace the placeholders. `DB_URL` must be a **JDBC** URL, for example `jdbc:postgresql://YOUR_HOST:5432/postgres?sslmode=require`. The Supabase project/API URL is not the database JDBC URL. Use the host and username from **Connect** in your Supabase dashboard. Direct connection works where IPv6 is available; the session pooler on port 5432 works on IPv4-only networks. Use a disposable database or schema while experimenting.
2. Load the environment variables and start the API:

   ```bash
   set -a
   source .env
   set +a
   mvn spring-boot:run
   ```

3. The API listens on `http://localhost:8080`. On the first run, Flyway creates the schema. Hibernate validates the schema on startup; it does not generate tables itself.

The `.env` file is ignored by Git. For a local PostgreSQL instance, set `DB_URL` to its JDBC URL and use its normal database username/password. Do not add a Supabase API key; this application connects through JDBC.

When transferring the project to another laptop, zip the source folder without `.env`, `.m2/`, or `target/`. Recreate `.env` from `.env.example` on the other machine, then run `mvn test` and `mvn spring-boot:run` there.

### Demo identities

| Username | Role | Password variable | Default for local demo |
| --- | --- | --- | --- |
| `admin` | `ADMIN` | `APP_ADMIN_PASSWORD` | `adminpass` |
| `alice` | `CUSTOMER` | `APP_ALICE_PASSWORD` | `alicepass` |
| `bob` | `CUSTOMER` | `APP_BOB_PASSWORD` | `bobpass` |

Set the password variables in `.env` before sharing or running the app. Passwords are encoded in memory with BCrypt. There is no signup or user table because the brief asks for basic role-based access control, not account management. Public catalog reads need no login. Booking and inbox endpoints require a customer login; admin endpoints require the admin login.

## Main API

All request/response bodies are JSON and all paths begin with `/api`. IDs in the examples are illustrative. Timestamps use ISO 8601 UTC; money uses decimal values.

| Access | Endpoints |
| --- | --- |
| Public | `GET /cities`, `/cities/{id}`, `/theaters?cityId=`, `/theaters/{id}`, `/movies`, `/movies/{id}`, `/shows?cityId=&date=&page=&size=`, `/shows/{id}`, `/shows/{id}/seats` |
| Admin | `GET/POST /admin/cities`, `/admin/theaters`, `/admin/movies`, `/admin/refund-policies`, `/admin/discount-codes`, `/admin/shows`; `GET/PUT/DELETE /admin/{resource}/{id}`; `GET/POST /admin/theaters/{id}/seats`; `GET/PUT/DELETE /admin/theaters/{id}/seats/{seatId}` |
| Customer | `POST /bookings/holds`, `POST /bookings/{id}/payments`, `POST /bookings/{id}/cancellation`, `GET /bookings`, `GET /bookings/{id}`, `GET /notifications` |

`GET /shows` lists future shows. The `date` filter is interpreted in the city's time zone, so it requires `cityId`. Lists of shows, bookings, and notifications accept zero-based `page` and `size` (maximum 100).

### Walkthrough

Use a fresh database or replace the example IDs with the IDs returned by your requests. Set a future show time in UTC.

```bash
# Create the catalog as admin.
curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"name":"Bengaluru","timeZone":"Asia/Kolkata"}' http://localhost:8080/api/admin/cities

curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"cityId":1,"name":"Central Cinema"}' http://localhost:8080/api/admin/theaters

curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"label":"A1","tier":"REGULAR"}' http://localhost:8080/api/admin/theaters/1/seats

curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"title":"Example Movie","durationMinutes":120}' http://localhost:8080/api/admin/movies

curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"name":"Standard","fullRefundHours":24,"partialRefundHours":2,"partialPercent":50}' \
  http://localhost:8080/api/admin/refund-policies

curl -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"movieId":1,"theaterId":1,"refundPolicyId":1,"startsAt":"2030-11-02T12:00:00Z","regularPrice":200,"premiumPrice":300,"weekendSurchargePercent":20}' \
  http://localhost:8080/api/admin/shows

# Browse, hold, pay, inspect, and cancel as alice. Replace IDs if your database already has data.
curl 'http://localhost:8080/api/shows?cityId=1&date=2030-11-02'
curl http://localhost:8080/api/shows/1/seats
curl -u "alice:$APP_ALICE_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"showId":1,"seatIds":[1]}' http://localhost:8080/api/bookings/holds
curl -u "alice:$APP_ALICE_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"outcome":"SUCCESS"}' http://localhost:8080/api/bookings/1/payments
curl -u "alice:$APP_ALICE_PASSWORD" http://localhost:8080/api/bookings
curl -u "alice:$APP_ALICE_PASSWORD" -X POST http://localhost:8080/api/bookings/1/cancellation
curl -u "alice:$APP_ALICE_PASSWORD" http://localhost:8080/api/notifications
```

The notification job runs every five seconds. A confirmation or cancellation may appear in `GET /notifications` shortly after the corresponding booking request returns. A reminder is queued when a confirmed show's start is within one hour.

## Booking rules and concurrency

Each show gets one `show_seats` row for each seat in its theater. The `show_seats` row is the allocation authority. A hold locks selected rows with `PESSIMISTIC_WRITE` inside one database transaction and always locks seats in ID order. A second customer waits for the lock and then sees `HELD` or `BOOKED`, so it receives `409 Conflict` instead of the same seat. The database also enforces a unique `(show_id, seat_id)` pair. Admin show creation and seat-layout edits lock the theater row, so an overlapping show or incomplete seat snapshot cannot slip through during a concurrent edit.

The hold is valid for five minutes or until the show starts, whichever comes first. A scheduled job marks old holds `EXPIRED` and releases their seats. Availability and the hold/payment paths enforce the deadline directly, so a delayed job cannot extend it. Expired seats can be reclaimed before cleanup runs. Booking, payment, cancellation, and seat changes each happen in transactions. Repeating a successful payment does not create another payment; repeating a cancellation does not create another refund.

Regular/premium are seat tiers. A show's weekend surcharge applies based on the city's IANA time zone, then an optional percentage discount applies to the subtotal. Prices are frozen on the hold. Refund-policy values are frozen when payment confirms the booking: full refund before the full cutoff, partial refund before the partial cutoff, zero thereafter until show start. Cancellation at or after show start is rejected.

Notifications are stored as pending database rows during confirmation/cancellation, then delivered to the in-app inbox by a background job. Reminder creation also runs in the background. This keeps delivery out of the booking response path. The job rechecks booking status before delivering a reminder and drops a pending reminder if the booking was cancelled. A failed job transaction leaves rows pending for the next run.

## Assumptions and limits

- Payment and refund are **simulated ledger records**. The API accepts `SUCCESS` or `FAILURE`; it does not handle cards or move money. This makes the flow demonstrable without payment-provider credentials.
- Notifications are delivered to the customer through `GET /notifications`, not email, SMS, or push. The local scheduler runs only while the app is running; a reminder can be missed if it is stopped for the entire reminder window.
- Seat layouts are locked once a theater has a show. A show with any booking cannot be edited or deleted. City time zones and theater cities cannot change after shows exist. These rules keep existing bookings meaningful.
- Discount codes have one percentage, a validity window, and an active flag. They do not stack and have no usage quota.
- A cancellation before show start is allowed even when the policy yields zero refund. Refund calculations use the amount paid, including discount.
- There are no taxes, convenience fees, seat maps, user registration, external integrations, distributed services, frontend, deployment files, or production monitoring. These are outside the chosen local scope.

## Tests

```bash
mvn test
```

Tests use H2 with the same Flyway migration, so no Supabase credentials are needed. They cover pricing and refund math, catalog/admin access, a full booking/cancellation flow, discount/payment/notification behavior, expiry and reclaim, ownership, and a two-customer same-seat race. On this machine's restricted environment Maven uses `mvn -Dmaven.repo.local=.m2/repository test`; on a normal laptop `mvn test` is sufficient.

H2 does not prove PostgreSQL's exact locking behavior. Before submission, run the walkthrough and this optional same-seat race against a **disposable** PostgreSQL or Supabase test database:

```bash
export TEST_DB_URL='jdbc:postgresql://YOUR_TEST_HOST:5432/postgres?sslmode=require'
export TEST_DB_USERNAME='YOUR_TEST_USER'
export TEST_DB_PASSWORD='YOUR_TEST_PASSWORD'
mvn -Dtest=PostgresConcurrencyIntegrationTest test
```

The optional test runs Flyway and writes test rows to the selected database, so use a database or schema meant for tests. A PostgreSQL smoke run is not claimed in this repository because no connection credentials were provided.

## What to explain in the recording

In under ten minutes: explain the single-service model, `show_seats` row lock and hold expiry, price/refund snapshots, the local payment and notification assumptions, Basic Auth role rules, and the tests. The project owner is handling the GitHub repository, commit history, and recording.
