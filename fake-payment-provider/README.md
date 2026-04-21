

## 🔐 Authentication

All endpoints (except `/webhooks`) require Basic Auth:


merchant_001:test_secret


Base64:

Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0


---

# 💳 Transactions

## ➕ Create Transaction

```bash
curl --location 'http://localhost:8888/api/v1/transactions' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0' \
--header 'Content-Type: application/json' \
--data '{
  "amount": 100,
  "currency": "USD",
  "method": "CARD",
  "description": "test payment",
  "externalId": "tx-123",
  "notificationUrl": "http://localhost:9999/webhook-test"
}'

📌 Notes:

externalId = idempotency key
same externalId → duplicate request returns same transaction
📄 Get Transactions (by date)
curl --location 'http://localhost:8888/api/v1/transactions?start_date=2026-04-01T00:00:00Z&end_date=2026-04-30T23:59:59Z' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0'
🔍 Get Transaction by ID
curl --location 'http://localhost:8888/api/v1/transactions/1' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0'
💸 Payouts
➕ Create Payout
curl --location 'http://localhost:8888/api/v1/payouts' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0' \
--header 'Content-Type: application/json' \
--data '{
  "amount": 50,
  "currency": "USD",
  "externalId": "payout-123",
  "notificationUrl": "http://localhost:9999/webhook-test"
}'

📌 Notes:

webhook will be triggered after creation
📄 Get All Payouts
curl --location 'http://localhost:8888/api/v1/payouts' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0'
🔍 Get Payout by ID
curl --location 'http://localhost:8888/api/v1/payouts/1' \
--header 'Authorization: Basic bWVyY2hhbnRfMDAxOnRlc3Rfc2VjcmV0'
🔔 Webhooks
📥 Receive Webhook (public endpoint)
curl --location 'http://localhost:8888/webhooks' \
--header 'Content-Type: application/json' \
--data '{
  "eventType": "TEST_EVENT",
  "entityId": 1,
  "payload": {
    "test": "data"
  },
  "notificationUrl": "http://localhost:9999/webhook-test"
}'
🧪 Test Webhook Receiver
curl --location 'http://localhost:9999/webhook-test' \
--header 'Content-Type: application/json' \
--data '{
  "hello": "world"
}'
🧪 Local Webhook Testing

Run simple HTTP server:

Node.js
npx http-server -p 9999
Python
python -m http.server 9999
🧠 Behavior
Idempotency
externalId ensures no duplicate transactions/payouts
repeated request returns existing entity
Flow
Transaction
Client → API
API → save transaction
API → return response
Payout
Client → API
API → save payout
API → send webhook
API → store webhook in DB