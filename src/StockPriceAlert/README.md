# Stock Price Alert Notification System — High-Level Design

## Problem Statement

Given an external source of stock prices (e.g., NASDAQ, NYSE, or a data broker) that provides real-time data:

```json
{
  "symbol": "UBER",
  "price": 99.01,
  "closing_price": 85.04
}
```

And a set of users registered on the platform who are subscribed to specific stocks — design a system that **sends a push notification** to all subscribed users when a stock's price jumps or drops **X%** compared to its closing price (daily/weekly, etc.).

---

## 1. Functional Requirements

| #    | Requirement |
|------|-------------|
| FR-1 | Users can **register/login** to the platform. |
| FR-2 | Users can **subscribe/unsubscribe** to one or more stock symbols (e.g., AAPL, UBER). |
| FR-3 | Users can **configure alert thresholds** — e.g., notify me when price moves ±5 % from closing price. (If not configured, a system-wide default like 5 % is used.) |
| FR-4 | The system **ingests real-time stock price data** from an external feed (NASDAQ, NYSE, data broker). |
| FR-5 | When a stock's current price deviates by ≥ X % from its closing price, the system **sends a push notification** to every user subscribed to that stock. |
| FR-6 | Users can choose the **closing-price baseline** — daily close, weekly close, etc. |
| FR-7 | The system should **not spam** — once a notification is sent for a particular stock crossing the threshold, it should not re-notify until the price returns within range and crosses again (or a cooldown period elapses). |

---

## 2. Non-Functional Requirements

| #     | Requirement | Target |
|-------|-------------|--------|
| NFR-1 | **Low latency** — notification delivered within **< 2 seconds** of threshold breach. | < 2 s end-to-end |
| NFR-2 | **High availability** — 99.9 % uptime. Stock markets are time-sensitive; downtime = missed alerts. | 99.9 % |
| NFR-3 | **Scalability** — support **10 M users**, **10 K stock symbols**, peak **100 K price updates/sec**. | Horizontal scaling |
| NFR-4 | **At-least-once delivery** — push notifications must not be silently lost. | Retry + DLQ |
| NFR-5 | **Idempotent notifications** — duplicate processing should not lead to duplicate user notifications. | Dedup key per event |
| NFR-6 | **Fault tolerance** — failure of one component should not bring down the entire pipeline. | Kafka + consumer groups |
| NFR-7 | **Extensibility** — easy to add new notification channels (SMS, email) later. | Plugin / strategy pattern |

---

## 3. Core Entities

```
User
├── userId        (PK, UUID)
├── name
├── email
├── deviceToken   (for push notifications — FCM / APNs)
└── createdAt

Subscription
├── subscriptionId (PK, UUID)
├── userId         (FK → User)
├── symbol         (e.g., "AAPL")
├── thresholdPct   (default 5.0)
├── baselineType   (DAILY_CLOSE | WEEKLY_CLOSE)
└── createdAt

Stock
├── symbol            (PK, e.g., "UBER")
├── currentPrice
├── dailyClosingPrice
├── weeklyClosingPrice
└── lastUpdatedAt

Notification
├── notificationId (PK, UUID)
├── userId         (FK → User)
├── symbol
├── triggerPrice
├── closingPrice
├── percentChange
├── sentAt
└── status         (SENT | FAILED | PENDING)
```

### Entity-Relationship Diagram

```
┌──────────┐       ┌───────────────┐       ┌──────────┐
│   User   │ 1───M │ Subscription  │ M───1 │  Stock   │
└──────────┘       └───────────────┘       └──────────┘
     │                                           │
     │ 1                                         │
     │                                           │
     └───────── M ┌───────────────┐              │
                  │ Notification  │──────────────┘
                  └───────────────┘
```

---

## 4. APIs

### 4.1 User & Subscription Service (REST)

| Method | Endpoint | Request Body | Description |
|--------|----------|-------------|-------------|
| `POST` | `/api/v1/users/register` | `{ name, email, deviceToken }` | Register a new user |
| `POST` | `/api/v1/users/login` | `{ email, password }` | Authenticate user, return JWT |
| `POST` | `/api/v1/subscriptions` | `{ symbol, thresholdPct?, baselineType? }` | Subscribe to a stock |
| `DELETE` | `/api/v1/subscriptions/{subscriptionId}` | — | Unsubscribe from a stock |
| `GET` | `/api/v1/subscriptions?userId={id}` | — | List user's active subscriptions |
| `PUT` | `/api/v1/subscriptions/{subscriptionId}` | `{ thresholdPct?, baselineType? }` | Update alert threshold or baseline |
| `GET` | `/api/v1/stocks/{symbol}` | — | Get current stock info |

### 4.2 Internal / System APIs

| Type | Endpoint / Topic | Description |
|------|-----------------|-------------|
| Kafka topic | `stock-prices` | External feed → ingested price ticks, partitioned by `symbol` |
| Kafka topic | `alerts` | Threshold-breached alert events `{ symbol, price, closingPrice, pctChange }` |
| Internal | `POST /internal/notifications/send` | Notification service → FCM / APNs gateway |

---

## 5. Back-of-the-Envelope Calculations

### 5.1 Assumptions

| Parameter | Value |
|-----------|-------|
| Total registered users | 10 M |
| Active stocks tracked | 10 K |
| Avg subscriptions per user | 5 |
| Total subscriptions (rows) | 50 M |
| Price updates per second (peak, market hours) | 100 K |
| Avg subscribers per stock | 50 M / 10 K = **5,000** |
| Stocks breaching threshold per day | ~1 % of stocks = **100** |
| Notifications per breach event | ~5,000 users |
| Total notifications per day | 100 × 5,000 = **500 K** |
| Peak burst (market open / volatile event) | ~50 K notifications in a few seconds |

### 5.2 Storage Estimates

| Data | Calculation | Size |
|------|-------------|------|
| User table | 10 M rows × 500 B | **~5 GB** |
| Subscription table | 50 M rows × 200 B | **~10 GB** |
| Stock table | 10 K rows × 200 B | **~2 MB** (easily cached) |
| Notification history | 500 K/day × 300 B × 365 days | **~55 GB/year** |

### 5.3 Throughput Estimates

| Component | Throughput |
|-----------|-----------|
| Price ingestion → Kafka | 100 K msg/sec (Kafka handles easily with partitioning) |
| Alert evaluation | 100 K evals/sec — in-memory comparison, sub-ms per tick |
| Subscription lookup (symbol → users) | Hot path → Redis: O(1) per symbol, returns up to 5 K user IDs |
| Push notification fan-out (peak burst) | ~50 K/sec → need 50–100 horizontal notification workers |
| FCM/APNs API calls | ~50 K/sec → batching (FCM supports 500 tokens/request) → ~100 API calls/sec |

### 5.4 Bandwidth

| Flow | Bandwidth |
|------|-----------|
| Incoming price feed | 100 K × 200 B = **20 MB/s** |
| Kafka internal replication | ~60 MB/s (3× replication) |
| Redis → Fan-out (subscriber lists) | Bursty: 5 K user IDs × 100 alerts/sec × 16 B = **8 MB/s** |
| Outbound push notifications | Negligible (small JSON payloads via FCM/APNs) |

---

## 6. High-Level Design

### 6.1 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL FEED                                      │
│                    (NASDAQ / NYSE / Data Broker)                            │
│                       WebSocket / REST API                                  │
└───────────────────────────┬─────────────────────────────────────────────────┘
                            │ price ticks (JSON)
                            ▼
┌───────────────────────────────────────┐
│       PRICE INGESTION SERVICE         │
│                                       │
│  • Connects to external feed          │
│  • Normalizes data format             │
│  • Publishes to Kafka                 │
│  • Handles reconnection / backfill    │
└───────────────────┬───────────────────┘
                    │
                    ▼
┌───────────────────────────────────────┐
│           APACHE KAFKA                │
│                                       │
│  Topic: stock-prices                  │
│  (partitioned by symbol hash)         │
│                                       │
│  Topic: alerts                        │
│  (partitioned by symbol hash)         │
└──────┬─────────────────────┬──────────┘
       │                     │
       ▼                     │
┌──────────────────────┐     │
│  ALERT EVALUATOR     │     │
│  SERVICE             │     │
│  (Consumer Group)    │     │
│                      │     │
│  For each price tick:│     │
│  1. Read tick        │     │
│  2. Get closing      │     │
│     price from Redis │     │
│  3. Compute Δ%       │     │
│  4. If |Δ%| ≥ min    │     │
│     threshold:       │     │
│     a. Check cooldown│     │
│     b. Publish alert │     │
│        to Kafka      │     │
│     c. Set cooldown  │     │
│        key in Redis  │     │
└──────────┬───────────┘     │
           │ alert events    │
           ▼                 ▼
┌───────────────────────────────────────┐
│      NOTIFICATION FAN-OUT SERVICE     │
│         (Consumer Group)              │
│                                       │
│  For each alert:                      │
│  1. Query Redis:                      │
│     symbol → [userId1, userId2, ...]  │
│  2. For each user:                    │
│     a. Check user's thresholdPct      │
│     b. If |Δ%| ≥ user threshold:     │
│        enqueue notification job       │
└───────────────────┬───────────────────┘
                    │ notification jobs
                    ▼
┌───────────────────────────────────────┐
│       NOTIFICATION QUEUE              │
│       (SQS / Kafka topic)            │
│                                       │
│  { userId, deviceToken, symbol,       │
│    price, closingPrice, pctChange }   │
└───────────────────┬───────────────────┘
                    │
                    ▼
┌───────────────────────────────────────┐
│    PUSH NOTIFICATION WORKERS          │
│    (Horizontally scaled pool)         │
│                                       │
│  • Dequeue job                        │
│  • Send push via FCM (Android)        │
│    or APNs (iOS)                      │
│  • On success: mark SENT in DB        │
│  • On failure: retry (exp backoff)    │
│    → after max retries: DLQ           │
│  • Write audit log to Notification DB │
└───────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════════════
                        USER-FACING LAYER
═══════════════════════════════════════════════════════════════════════════════

┌───────────────┐     ┌──────────────────────┐     ┌───────────────────┐
│  Mobile App / │────▶│     API GATEWAY      │────▶│  SUBSCRIPTION     │
│  Web Client   │     │  (Auth, Rate Limit)  │     │  SERVICE          │
└───────────────┘     └──────────────────────┘     │                   │
                                                    │  CRUD on subs     │
                                                    │  User management  │
                                                    └────────┬──────────┘
                                                             │
                                          ┌──────────────────┼──────────────┐
                                          │                  │              │
                                          ▼                  ▼              ▼
                                   ┌────────────┐    ┌────────────┐  ┌──────────┐
                                   │ Postgres   │    │   Redis    │  │ Stock DB │
                                   │ (Users &   │───▶│  Cache     │  │ (closing │
                                   │  Subs)     │CDC │            │  │  prices) │
                                   └────────────┘    │ • symbol   │  └──────────┘
                                                     │   → users  │
                                                     │ • closing  │
                                                     │   prices   │
                                                     │ • cooldown │
                                                     │   flags    │
                                                     └────────────┘
```

### 6.2 Component Breakdown

| Component | Responsibility | Tech Choices |
|-----------|---------------|--------------|
| **Price Ingestion Service** | Connects to external feed via WebSocket; normalizes data; publishes to Kafka `stock-prices` topic. | Java / Go, Kafka Producer SDK |
| **Apache Kafka** | Message broker — decouples ingestion from evaluation; buffers bursts; enables replay. Two topics: `stock-prices` and `alerts`, both partitioned by symbol. | Apache Kafka (3+ brokers) |
| **Alert Evaluator Service** | Consumes price ticks; compares against closing price from Redis; if Δ% ≥ minimum threshold → publishes to `alerts` topic. Manages cooldown state via Redis TTL keys. | Java / Go, Kafka Consumer Group |
| **Redis Cache** | (a) Stock closing prices (b) `symbol → [userIds]` mapping (c) Cooldown / dedup flags with TTL. | Redis Cluster |
| **Notification Fan-out Service** | Consumes alert events; looks up subscribers from Redis; filters by per-user threshold; enqueues individual notification jobs. | Java / Go, Kafka Consumer Group |
| **Notification Queue** | Buffers per-user push notification jobs; absorbs bursts; supports retries and DLQ. | AWS SQS or dedicated Kafka topic |
| **Push Notification Workers** | Dequeues jobs; sends push via FCM (Android) / APNs (iOS); retries on transient failures; writes audit log. | Worker pool, FCM / APNs SDK |
| **Subscription Service** | User-facing REST API for registration and subscription management. Writes to Postgres; syncs to Redis via CDC (Debezium). | Spring Boot / Express, Postgres |
| **API Gateway** | Authentication (JWT), rate limiting, request routing. | AWS API Gateway / Kong / Nginx |
| **Stock DB** | Source of truth for closing prices — updated at market close by a scheduled cron job or EOD feed. | Postgres or DynamoDB |
| **Notification DB** | Audit trail of all sent notifications for debugging, analytics, and compliance. | Postgres or DynamoDB |

### 6.3 Data Flow — Happy Path

```
Step 1: External feed pushes price tick → Price Ingestion Service → Kafka (stock-prices)

Step 2: Alert Evaluator consumes tick for UBER:
        price = $99.01, closing_price = $85.04
        Δ% = (99.01 − 85.04) / 85.04 × 100 = +16.4%
        Minimum threshold across all UBER subscribers = 5%
        16.4% ≥ 5% → CHECK cooldown in Redis → No cooldown exists
        → Publish to Kafka (alerts): { symbol: "UBER", price: 99.01,
                                        closingPrice: 85.04, pctChange: 16.4 }
        → SET Redis key "cooldown:UBER" with TTL = 15 min

Step 3: Fan-out Service consumes alert:
        → GET Redis key "subs:UBER" → [user1, user2, ..., user5000]
        → For each user, check their configured threshold:
            user1: threshold = 10% → 16.4 ≥ 10 ✓ → enqueue
            user2: threshold = 20% → 16.4 < 20 ✗ → skip
            ...
        → Enqueue ~4,000 notification jobs to SQS

Step 4: Push Workers dequeue jobs → batch-call FCM/APNs
        → User receives: "🚨 UBER is up 16.4% from closing ($85.04 → $99.01)"
        → Write { notificationId, userId, symbol, status: SENT } to Notification DB

Step 5: After 15 min, cooldown key expires in Redis.
        If UBER is still above threshold and a new tick arrives → re-evaluate → re-notify.
```

### 6.4 Key Design Decisions

#### 1. Kafka Partitioned by Symbol
All price ticks for the same stock go to the **same Kafka partition**. This ensures a single consumer instance in the Alert Evaluator group handles all ticks for that symbol — no race conditions on cooldown state, and ordered processing per symbol.

#### 2. Two-Stage Notification Pipeline
Separating **fan-out** (who to notify) from **delivery** (how to notify) allows independent scaling:
- Fan-out is CPU-bound (lookups, filtering) — scale based on alert volume.
- Delivery is I/O-bound (FCM/APNs HTTP calls) — scale based on notification volume.

#### 3. Cooldown / Dedup via Redis TTL
After an alert fires for symbol X, set a Redis key `cooldown:X` with a TTL (e.g., 15 min). The Alert Evaluator checks this key before publishing. This prevents notification spam during volatile periods.

#### 4. CDC for Cache Consistency
When a user subscribes or unsubscribes, the Postgres change is propagated to Redis via **Change Data Capture** (Debezium / application-level events). This keeps the hot `symbol → userIds` mapping in Redis consistent with the source of truth without coupling the subscription API to Redis writes.

#### 5. Per-User Threshold Handling
The Alert Evaluator fires at the **minimum threshold** across all subscribers for a stock (e.g., if thresholds are 5%, 10%, 15%, fire at 5%). The Fan-out Service then filters per-user — only users whose threshold is met receive a notification. This avoids evaluating N different thresholds per price tick.

#### 6. Graceful Degradation
- If Redis is down → fall back to Postgres (higher latency, but functional).
- If FCM/APNs is down → notifications stay in the queue; workers retry with exponential backoff.
- If Kafka lags → consumers catch up; notifications may be slightly delayed but not lost.

### 6.5 Scaling Strategies

| Component | Scaling Approach |
|-----------|-----------------|
| Kafka | Add partitions per topic; add brokers for throughput |
| Alert Evaluator | Horizontal scaling via consumer group (1 consumer per partition) |
| Fan-out Service | Horizontal scaling via consumer group |
| Push Workers | Auto-scale based on queue depth (SQS → Lambda or ECS tasks) |
| Redis | Redis Cluster with read replicas |
| Postgres | Read replicas for subscription reads; partition Notification table by date |

### 6.6 Failure Handling

| Failure | Mitigation |
|---------|-----------|
| External feed disconnects | Auto-reconnect with backoff; alert ops team; backfill missed ticks |
| Kafka broker failure | Replication factor ≥ 3; ISR-based leader election |
| Alert Evaluator crash | Kafka consumer group rebalances; other consumers pick up partitions |
| Redis failure | Circuit breaker → fall back to DB; Redis Cluster with replicas |
| FCM/APNs failure | Exponential backoff retry (3 attempts); Dead Letter Queue for persistent failures |
| Duplicate processing | Idempotency key = `{symbol}:{userId}:{alertTimestamp}` — dedup at notification write |

---

## Summary

This system follows an **event-driven, streaming architecture** built around Kafka as the central nervous system. The pipeline is:

```
Feed → Ingest → Kafka → Evaluate → Kafka → Fan-out → Queue → Deliver
```

Each stage is independently scalable, fault-tolerant, and loosely coupled. The use of Redis for hot data (closing prices, subscriber lists, cooldowns) ensures sub-millisecond lookups on the critical path, while Postgres serves as the durable source of truth.
