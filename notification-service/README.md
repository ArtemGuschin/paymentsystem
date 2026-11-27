# Notification Service

Notification Service is a Java 21 / Spring Boot microservice responsible for consuming notification events from Kafka, persisting notifications in PostgreSQL, delivering registration emails asynchronously, and exposing a protected internal REST API.

The service uses persistent email delivery state, retries, and a Kafka Dead Letter Topic (DLT) to improve reliability.

## Technology Stack

- Java 21
- Spring Boot 3.5
- Spring Web and Spring Security
- Spring Data JPA and JDBC
- PostgreSQL and Flyway
- Apache Kafka and Apache Avro
- Confluent Schema Registry
- Spring Mail and MailHog
- Testcontainers, JUnit 5, and Mockito
- Docker / Docker Compose

## Architecture

Notification events are published through the Transactional Outbox pattern.

Registration flow:

```text
User Registration
       |
       v
Individuals API
       |
       v
Individuals PostgreSQL
(notification_outbox)
       |
       v
Outbox Publisher
       |
       v
Kafka: notification.created
       |
       v
NotificationCreatedConsumer
       |
       v
NotificationService.process()
       |
       v
Notification PostgreSQL
(notification saved, email PENDING)
       |
       v
EmailDeliveryPublisher
       |
       v
EmailService
       |
       v
SMTP / MailHog
```

The registration request does not send email synchronously.

The Kafka consumer persists the notification and its initial email delivery state. A separate scheduled component performs email delivery.

Webhook Collector Service can also publish `notification.created` events through its own Outbox.

## Notification Event

Events use the Avro contract:

```text
common-library/src/main/avro/NotificationCreated.avsc
```

Main fields:

```text
eventId
userUid
notificationType
email
message
subject
createdBy
createdAt
```

Supported registration notification type:

```text
USER_REGISTERED
```

The `eventId` identifies the event and is used to prevent duplicate notification records.

## Kafka Consumer

Source topic:

```text
notification.created
```

Consumer group:

```text
notification-service
```

The consumer uses:

```text
auto-offset-reset: earliest
enable-auto-commit: false
ErrorHandlingDeserializer
KafkaAvroDeserializer
```

`NotificationCreatedConsumer` delegates processing to `NotificationService.process()`.

The service checks the event ID before inserting a notification. The database also enforces uniqueness of `event_id`.

### Kafka Retry and Dead Letter Topic

Kafka processing failures are handled by `NotificationKafkaErrorConfig`.

Retry configuration:

```text
Retry interval: 1 second
Additional retry attempts: 3
```

After retries are exhausted, the failed record is sent to:

```text
notification.created.DLT
```

The DLT producer uses Kafka producer acknowledgements and idempotence.

A record is not considered successfully recovered until the DLT publication has been confirmed.

Kafka processing retries and email delivery retries are independent mechanisms.

## Database

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
user_uid
notification_type
notification_status
message
subject
created_by
recipient_email
created_at
modified_at
email_sent_at
email_delivery_status
email_attempt_count
email_next_attempt_at
email_last_error
```

The `event_id` column has a unique constraint.

Flyway migrations:

```text
src/main/resources/db/migration/V1__create_notifications.sql
src/main/resources/db/migration/V2__add_email_sent_at.sql
src/main/resources/db/migration/V3__add_email_delivery_state.sql
```

Flyway manages database schema changes during application startup.

## Asynchronous Email Delivery

Registration emails are delivered by:

```text
EmailDeliveryPublisher
```

The publisher runs periodically:

```text
Default scheduling delay: 5000 ms
Batch size: 20
Maximum delivery attempts: 10
Delivery lease: 2 minutes
```

### Email Delivery States

| Status | Meaning |
|---|---|
| NOT_REQUIRED | Email delivery is not required |
| PENDING | Email is waiting for delivery or retry |
| SENDING | Email delivery has been reserved by a publisher |
| SENT | SMTP delivery was accepted and the state was saved |
| FAILED | Maximum delivery attempts were exhausted |

For `USER_REGISTERED`, a new notification starts with:

```text
notification_status = NEW
email_delivery_status = PENDING
```

### Delivery Process

```text
PENDING
   |
   v
Atomic claim
   |
   v
SENDING
   |
   +---- SMTP success ----> SENT
   |
   +---- SMTP error ------> PENDING
                                  |
                                  v
                                Retry
                                  |
                                  v
                         Maximum attempts
                                  |
                                  v
                                FAILED
```

The publisher uses database-level conditional updates to prevent multiple workers from claiming the same available delivery simultaneously.

Expired `SENDING` leases can be reclaimed.

### Email Retry

Failed deliveries are retried with exponential backoff, capped at 300 seconds.

The delivery state and error details are persisted in PostgreSQL.

The `email_sent_at` field is populated after SMTP accepts the message and the successful delivery state is saved.

### Delivery Guarantee

Email delivery is designed for retryable, at-least-once processing.

Strict exactly-once SMTP delivery is not guaranteed.

For example, SMTP may accept an email while the subsequent PostgreSQL update fails. A later retry may then send the email again.

## Internal REST API

Notification Service exposes an internal REST API.

Application port:

```text
8787
```

Base path:

```text
/api/v1/notifications
```

### Authentication and Authorization

REST endpoints require HTTP Basic Authentication.

Required Spring Security role:

```text
NOTIFICATION_INTERNAL
```

Credentials are configured using environment variables:

```text
NOTIFICATION_INTERNAL_USERNAME
NOTIFICATION_INTERNAL_PASSWORD
```

Requests without the required authentication or role are rejected.

### Get Notifications

```http
GET /api/v1/notifications/{userUid}
```

Returns notifications belonging to the specified user.

### Complete Notification

```http
PATCH /api/v1/notifications/{id}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "COMPLETED"
}
```

Only the transition to `COMPLETED` is supported.

Repeated requests to complete an already completed notification are idempotent.

The notification's business status is separate from its email delivery status.

## Docker Deployment

Main Docker Compose file:

```text
Individuals/Individuals/docker-compose.yml
```

From the `Individuals/Individuals` directory:

```powershell
docker compose up -d notification-db kafka schema-registry mailhog notification-service
```

The Compose configuration also manages dependencies such as ZooKeeper.

Required internal API credentials must be configured before starting Notification Service.

Check containers:

```powershell
docker compose ps
```

Check logs:

```powershell
docker logs individuals-notification-service --tail 100
```

### Docker Networking

| Component | Host port | Docker network |
|---|---|---|
| Notification Service | Not published | notification-service:8787 |
| Notification PostgreSQL | 5439 | notification-db:5432 |
| Kafka | 9092 | kafka:29092 |
| Schema Registry | 8086 | schema-registry:8081 |
| MailHog SMTP | 1025 | mailhog:1025 |
| MailHog UI | 8025 | mailhog:8025 |

**Important:** Notification Service uses `expose: 8787`, not a host `ports` mapping.

Therefore, its REST API is not directly available at `localhost:8787` when running through the supplied Docker Compose configuration.

Internal Docker services can access:

```text
http://notification-service:8787
```

## Local Development

The application can also run directly from IntelliJ IDEA or Gradle.

From the `notification-service` directory:

```powershell
.\gradlew.bat bootRun
```

Required environment variables include:

```text
NOTIFICATION_INTERNAL_USERNAME
NOTIFICATION_INTERNAL_PASSWORD
```

For the Docker Compose infrastructure, configure:

```text
DB_URL=jdbc:postgresql://localhost:5439/notification_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SCHEMA_REGISTRY_URL=http://localhost:8086
MAIL_HOST=localhost
MAIL_PORT=1025
```

When running outside Docker, the application listens on:

```text
localhost:8787
```

## Tests

Run the complete test suite:

```powershell
.\gradlew.bat clean test --max-workers=1
```

The test suite covers notification business logic and persistence, including:

- Kafka event processing and idempotency
- Duplicate event handling
- Notification status transitions
- Protected REST endpoints
- PostgreSQL persistence and Flyway migrations
- Asynchronous email delivery
- Email retries and failure states
- Kafka retries and Dead Letter Topic handling

Testcontainers is used for database integration testing.

## Logging

Logging configuration:

```text
src/main/resources/logback-spring.xml
```

The service uses structured JSON logging.

Operational logs include notification event IDs, delivery attempts, retry scheduling, and delivery failures.

## Architecture Diagrams

PlantUML diagrams are available in:

```text
docs/sequence.puml
docs/container.puml
docs/component.puml
```

- `sequence.puml` — registration, Outbox, Kafka consumption, email delivery, retries, and internal REST interactions.
- `container.puml` — C4 container view of the notification architecture.
- `component.puml` — C4 component view of Notification Service internals.

## Reliability Summary

Notification Service separates three responsibilities:

1. **Event publication:** source services persist events through Transactional Outbox before publishing them to Kafka.
2. **Event processing:** Notification Service consumes Kafka events and persists notifications idempotently.
3. **Email delivery:** a scheduled publisher delivers pending emails and persists retry state.

This separation allows failed operations to be retried without requiring the entire registration flow to execute again.

The system provides durable processing and recovery mechanisms while acknowledging that external SMTP delivery cannot guarantee exactly-once behavior.