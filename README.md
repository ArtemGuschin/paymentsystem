# Payment System

Учебный многосервисный платёжный стенд на Java и Spring Boot.

Система моделирует полный жизненный цикл платежа: от запроса клиента до обращения к платёжному провайдеру, получения webhook, публикации события в Kafka и финального обновления транзакции.

## Архитектура

Основной поток обработки платежа:

```text
Individuals API
      ↓
Payment Service
      ↓
Fake Payment Provider
      ↓
Webhook Collector
      ↓
Transactional Outbox
      ↓
Kafka
      ↓
Transaction Service
      ↓
Transaction status / Wallet
```

Основные сервисы:

- `Individuals API` — внешний API системы.
- `Payment Service` — создание и управление платежами.
- `Fake Payment Provider` — эмуляция внешнего платёжного провайдера.
- `Webhook Collector` — приём webhook-уведомлений от провайдера.
- `Transaction Service` — обработка транзакций и кошельков.
- `Kafka` — асинхронная доставка событий.
- `Keycloak` — аутентификация и авторизация.
- `PostgreSQL` — хранение данных сервисов.
- `Prometheus` — сбор метрик.
- `Grafana` — визуализация метрик.

## Требования

Для локального запуска необходимы:

- Docker Desktop
- Docker Compose
- свободные порты, используемые стендом

Для сборки `Individuals API` также должен быть доступен локальный Nexus:

```text
http://localhost:8081
```

Он используется для внутренних артефактов проекта, включая OpenAPI-клиенты.

## Полный Docker-стенд

Полная конфигурация находится в:

```text
Individuals/Individuals/docker-compose.yml
```

Из корня проекта:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml up --build
```

Для запуска в фоне:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml up --build -d
```

## Сервисы и порты

| Сервис | Адрес |
|---|---|
| Individuals API | http://localhost:9090 |
| Keycloak | http://localhost:9091 |
| Kafka UI | http://localhost:8082 |
| Payment Service | http://localhost:8084 |
| Fake Payment Provider | http://localhost:8899 |
| Webhook Collector | http://localhost:7878 |
| Transaction Service | http://localhost:8585 |
| Prometheus | http://localhost:9095 |
| Grafana | http://localhost:3000 |
| Kafka | localhost:9092 |
| Zookeeper | localhost:2181 |

PostgreSQL:

| База | Порт |
|---|---:|
| payment_service | 5434 |
| fake_provider | 5435 |
| webhook_collector_db | 5436 |
| transaction_db_0 | 5437 |
| transaction_db_1 | 5438 |

## Kafka

В Docker используются два Kafka listener:

```text
localhost:9092
```

для приложений, запущенных локально из IDE, и:

```text
kafka:29092
```

для сервисов внутри Docker-сети.

Kafka UI:

```text
http://localhost:8082
```

Основной topic обработки статусов платежей:

```text
payment.status.updated
```

Для сообщений, которые не удалось обработать после повторных попыток, используется Dead Letter Topic:

```text
payment.status.updated.DLT
```

Необработанное сообщение проходит retry и после исчерпания попыток помещается в DLT.

Сообщение из DLT может быть повторно отправлено в основной topic один раз. Специальный header предотвращает бесконечный цикл:

```text
main → retry → DLT → main → retry → DLT → stop
```

## Payment flow

При создании платежа:

```text
Client
  ↓
Individuals API
  ↓
Payment Service
  ↓
Fake Payment Provider
```

После изменения статуса провайдер отправляет webhook:

```text
Fake Payment Provider
  ↓
Webhook Collector
  ↓
Outbox
  ↓
Kafka: payment.status.updated
  ↓
Transaction Service
```

`Transaction Service` обрабатывает финальный статус:

```text
PENDING → SUCCESS
```

или:

```text
PENDING → FAILED
```

Для успешного пополнения баланс кошелька изменяется только после подтверждённого успешного платежа.

## Надёжность обработки

В проекте реализованы:

- idempotency платежных запросов;
- проверка конфликта параметров при повторном idempotency key;
- Transactional Outbox;
- Kafka consumer с `auto-offset-reset=earliest`;
- retry обработки Kafka-событий;
- Dead Letter Topic;
- ограниченный DLT reprocessing;
- защита от повторной обработки финализированной транзакции;
- атомарное изменение баланса;
- восстановление платежа по `externalId`;
- сохранение `providerTransactionId` до ожидания финального статуса;
- обработка неизвестного результата при сетевой ошибке;
- денежные суммы через `BigDecimal`.

## Просмотр состояния контейнеров

```bash
docker compose -f Individuals/Individuals/docker-compose.yml ps
```

## Просмотр логов

Все сервисы:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml logs -f
```

Например, только Transaction Service:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml logs -f transaction-service
```

Webhook Collector:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml logs -f webhook-collector-service
```

Payment Service:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml logs -f payment-service
```

## Остановка стенда

```bash
docker compose -f Individuals/Individuals/docker-compose.yml down
```

Для полной очистки контейнеров и Docker volumes:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml down -v
```

> `down -v` удаляет данные PostgreSQL, хранящиеся в Docker volumes.

## Повторная сборка

После изменения исходного кода:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml up --build
```

При необходимости полной пересборки без Docker cache:

```bash
docker compose -f Individuals/Individuals/docker-compose.yml build --no-cache
docker compose -f Individuals/Individuals/docker-compose.yml up
```

## Основные технологии

- Java
- Spring Boot
- Spring Data JPA
- Spring Security
- PostgreSQL
- Apache Kafka
- ShardingSphere
- Flyway
- Keycloak
- OpenAPI
- MapStruct
- Docker Compose
- Testcontainers
- JUnit 5
- Mockito
- Prometheus
- Grafana