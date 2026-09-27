#!/usr/bin/env bash

# Load environment variables for passwords
set -a
source .env
set +a

echo "=== Phase 1: Admin Catalog Construction ==="
echo "Creating City..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"name":"Bengaluru","timeZone":"Asia/Kolkata"}' http://localhost:8080/api/admin/cities
echo -e "\n"
sleep 1

echo "Creating Theater..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"cityId":1,"name":"Central Cinema"}' http://localhost:8080/api/admin/theaters
echo -e "\n"
sleep 1

echo "Creating Seat Layout..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"label":"A1","tier":"REGULAR"}' http://localhost:8080/api/admin/theaters/1/seats
echo -e "\n"
sleep 1

echo "Creating Movie..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"title":"Example Movie","durationMinutes":120}' http://localhost:8080/api/admin/movies
echo -e "\n"
sleep 1

echo "Creating Refund Policy..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"name":"Standard","fullRefundHours":24,"partialRefundHours":2,"partialPercent":50}' \
  http://localhost:8080/api/admin/refund-policies
echo -e "\n"
sleep 1

echo "Creating Show..."
curl -s -u "admin:$APP_ADMIN_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"movieId":1,"theaterId":1,"refundPolicyId":1,"startsAt":"2030-11-02T12:00:00Z","regularPrice":200,"premiumPrice":300,"weekendSurchargePercent":20}' \
  http://localhost:8080/api/admin/shows
echo -e "\n"
sleep 2

echo "=== Phase 2: Public Inventory Browsing ==="
echo "Browsing Shows..."
curl -s 'http://localhost:8080/api/shows?cityId=1&date=2030-11-02'
echo -e "\n"
sleep 1

echo "Checking Seat Availability..."
curl -s http://localhost:8080/api/shows/1/seats
echo -e "\n"
sleep 2

echo "=== Phase 3: Customer Booking & Payment ==="
echo "Holding a Seat as Alice..."
curl -s -u "alice:$APP_ALICE_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"showId":1,"seatIds":[1]}' http://localhost:8080/api/bookings/holds
echo -e "\n"
sleep 1

echo "Simulating Successful Payment..."
curl -s -u "alice:$APP_ALICE_PASSWORD" -H 'Content-Type: application/json' \
  -d '{"outcome":"SUCCESS"}' http://localhost:8080/api/bookings/1/payments
echo -e "\n"
sleep 1

echo "Retrieving Alice's Booking History..."
curl -s -u "alice:$APP_ALICE_PASSWORD" http://localhost:8080/api/bookings
echo -e "\n"
sleep 2

echo "=== Phase 4: Customer Cancellation & Notifications ==="
echo "Cancelling the Booking..."
curl -s -u "alice:$APP_ALICE_PASSWORD" -X POST http://localhost:8080/api/bookings/1/cancellation
echo -e "\n"
sleep 1

echo "Checking Alice's Notification Inbox..."
curl -s -u "alice:$APP_ALICE_PASSWORD" http://localhost:8080/api/notifications
echo -e "\n"