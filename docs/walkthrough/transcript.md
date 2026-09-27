# Movie Ticket Booking code walkthrough



Synthetic English narration; source excerpts are taken from this project.



## 00:00 — One application, clear responsibilities



This walkthrough follows the movie ticket booking API from REST controllers through business logic to database transactions.

Controllers handle HTTP, services own rules, and repositories persist entities. Separate packages contain records, security, jobs, errors, and calculations.

MovieTicketsApplication starts Spring Boot and scheduling. Environment variables supply database credentials, and Flyway manages the schema.



## 00:27 — Security and the HTTP boundary



SecurityConfiguration uses HTTP Basic with admin and customer roles. Catalog browsing is public, while management and booking routes require the correct role.

BookingController exposes hold, pay, and cancel. It validates the body and passes the authenticated username to BookingService for ownership checks.

The get and history methods return the customer's own bookings. The inbox endpoint returns that customer's delivered notifications.



## 00:54 — Validate inputs, explain errors



ApiModels holds request and response records. Validation checks required fields, price precision, percentages, and seat-list size before business logic runs.

ApiException represents expected failures. ApiErrors maps invalid input to bad requests, and seat or database conflicts to conflict responses with useful messages.



## 01:16 — Create the catalog and show inventory



CatalogController delegates catalog management to CatalogService, covering cities, theaters, seats, movies, shows, pricing, discounts, and refund policies.

createShow locks the theater, validates the schedule, and creates a ShowSeat row for every seat. The lock also coordinates concurrent layout edits.

browseShows filters future shows by city and local date, with database pagination. Protected admin edits preserve the meaning of existing bookings.



## 01:45 — Entities retain state; repositories lock it



City, Theater, TheaterSeat, Movie, and MovieShow describe the catalog. Booking and BookingSeat retain ownership, totals, and seat-price snapshots for history.

Payment, Refund, and Notification store outcomes. ShowSeat is the allocation authority: it keeps availability, the current booking, and the hold deadline.

Repository interfaces handle persistence. ShowSeatRepository's lockOne and lockByBooking request database write locks, keeping competing updates inside transactions.



## 02:15 — Hold seats in one transaction



BookingService.hold checks distinct seat IDs, locks the show, and processes seats in sorted order.

Booked seats and unexpired holds are rejected, so the transaction cannot allocate the same seat twice.

It saves the booking and seat snapshots in the same transaction. The hold ends after five minutes, or when the show starts, whichever comes first.



## 02:36 — Calculate prices once, then snapshot them



PricingCalculator starts from the regular or premium seat price. seatPrice applies the weekend surcharge using the city's time zone and rounds to two decimal places.

A valid discount reduces the subtotal through applyDiscount. BookingService checks the code's active window and saves the result, so later price edits cannot change it.



## 02:55 — Pay, confirm, and make retries safe



pay locks the customer's booking and checks its deadline and continued ownership of every seat. A failed simulated payment keeps the booking held until expiry.

Success records payment, freezes the refund policy, confirms the booking, marks its seats booked, and queues a notification. Repeating success returns the existing result without another payment.



## 03:15 — Cancel and calculate the refund



cancel allows the owner to cancel a confirmed booking before the show starts. A repeated cancellation returns the existing result.

RefundCalculator.amount applies the saved cutoffs to the amount paid. The service records a simulated refund, releases seats, and queues cancellation in one transaction.



## 03:34 — Expire holds automatically



MaintenanceJobs finds overdue holds and calls expireOne. That method locks the booking, marks it expired, and releases seats that still belong to it.

Availability and booking operations also check expiry directly. Therefore, a delayed cleanup job cannot extend a hold, and another customer can reclaim an expired seat.



## 03:53 — Deliver notifications in the background



Confirmation and cancellation transactions call NotificationService.enqueue to store pending messages.

enqueueReminders finds confirmed bookings within one hour of show time, while background jobs handle delivery.

deliverPending rechecks reminders and drops them after cancellation. inbox returns delivered records to the customer. This is a REST inbox, with no external email or SMS provider.



## 04:16 — Tests, setup, and the verification limit



The tests cover calculations, ownership, expiry, refunds, notifications, and concurrent requests. This race test starts two customers and expects exactly one seat-hold winner.

The local suite has seventeen passing tests. The optional PostgreSQL race is unverified because no database credentials were supplied.

Run Maven tests, then set database variables and start the app using the README. Payments and refunds are simulated; the plan, agent instructions, and skills record document the development workflow.

