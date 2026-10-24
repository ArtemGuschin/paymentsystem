Notification Service
Notification Service is responsible for receiving notification events from Kafka,
persisting notifications, sending registration emails, and exposing REST endpoints
for notification retrieval and status updates.
Technology Stack
Java 21
Spring Boot 3.5.0
Spring Web
Spring Data JPA
PostgreSQL
Flyway
Apache Kafka
Apache Avro
Confluent Schema Registry
Spring Mail
MailHog
Testcontainers
JUnit 5
Mockito
Docker / Docker Compose
Logback JSON logging
---
Architecture
The service receives Avro events from Kafka topic:
```text
notification.created
```
The main flow:
```text
Individuals API
    ↓
Kafka topic: notification.created
    ↓
Notification Service
    ↓
PostgreSQL
    ↓
EmailService
    ↓
MailHog / SMTP
```
For `USER_REGISTERED` events, Notification Service sends a registration email.
---
Notification Event
Avro contract:
```text
common-library/src/main/avro/NotificationCreated.avsc
```
Main fields:
```text
eventId
notificationType
message
userUid
email
subject
createdBy
createdAt
```
Example event type:
```text
USER_REGISTERED
```
---
REST API
Base URL:
```text
http://localhost:8787/api/v1/notifications
```
Get notifications by user
```http
GET /api/v1/notifications/{userUid}
```
Complete notification
```http
PATCH /api/v1/notifications/{id}/status
```
Request body:
```json
{
  "status": "COMPLETED"
}
```
Only transition to `COMPLETED` is allowed. Repeated `COMPLETED` requests are idempotent.
---
Database
Database:
```text
notification_db
```
Main table:
```text
notifications
```
Important fields:
```text
id
event_id
created_at
modified_at
message
user_uid
notification_status
subject
created_by
notification_type
recipient_email
email_sent_at
```
`event_id` has a UNIQUE constraint and is used for Kafka event idempotency.
Flyway migrations:
```text
src/main/resources/db/migration/V1__create_notifications.sql
src/main/resources/db/migration/V2__add_email_sent_at.sql
```
---
Kafka
Topic:
```text
notification.created
```
Consumer group:
```text
notification-service
```
Local Kafka:
```text
localhost:9092
```
Docker Kafka:
```text
kafka:29092
```
Schema Registry local:
```text
http://localhost:8086
```
Schema Registry in Docker:
```text
http://schema-registry:8081
```
---
Email
Local SMTP is provided by MailHog.
SMTP:
```text
localhost:1025
```
MailHog UI:
```text
http://localhost:8025
```
Docker SMTP:
```text
mailhog:1025
```
Registration emails are sent for:
```text
notificationType = USER_REGISTERED
```
After successful email delivery, `email_sent_at` is populated.
---
Local Run
Run application:
```powershell
.\gradlew bootRun
```
Default port:
```text
8787
```
---
Docker
Build image:
```powershell
docker build -t notification-service:local .
```
Full compose:
```text
Individuals/Individuals/docker-compose.yml
```
Start Notification Service infrastructure:
```powershell
cd Individuals\Individuals

docker compose up -d `
  zookeeper `
  kafka `
  schema-registry `
  mailhog `
  notification-db `
  notification-service
```
Check containers:
```powershell
docker compose ps
```
Check logs:
```powershell
docker logs individuals-notification-service --tail 100
```
Docker ports:
```text
Notification Service  8787
Notification DB       5439
Kafka                  9092
Schema Registry        8086
MailHog UI             8025
MailHog SMTP           1025
```
---
Tests
Run all tests:
```powershell
.\gradlew clean test
```
Implemented:
```text
Unit tests
- NotificationService business logic
- NEW → COMPLETED
- repeated COMPLETED request
- notification not found
- duplicate event handling
- USER_REGISTERED email flow
- missing email validation

Integration tests
- PostgreSQL via Testcontainers
- Flyway migrations
- repository persistence
- UNIQUE event_id constraint

Smoke test
- Spring Application Context
```
---
JSON Logging
Logs are written in JSON format through Logback.
Configuration:
```text
src/main/resources/logback-spring.xml
```
Example:
```json
{
  "@timestamp": "2026-10-16T15:05:20",
  "message": "Notification processed",
  "level": "INFO",
  "service": "notification-service"
}
```
---
Diagrams
Architecture diagrams:
```text
docs/sequence.puml
docs/container.puml
docs/component.puml
```
---
Verified End-to-End Flow
```text
POST /v1/auth/registration
    ↓
Person Service
    ↓
Keycloak
    ↓
Access Token
    ↓
Individuals API Kafka Producer
    ↓
notification.created
    ↓
Kafka + Schema Registry
    ↓
Notification Service
    ↓
PostgreSQL
    ↓
EmailService
    ↓
MailHog
```
Verified behavior:
```text
registration succeeds
access token is returned
notification event is published
notification is persisted
registration email is delivered
email_sent_at is populated
duplicate eventId is not persisted twice
duplicate eventId does not send duplicate email
```