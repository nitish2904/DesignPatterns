# 📈 Stock Price Alert Notification System — HLD

## Problem Statement
Given an external source of stock prices (NASDAQ, NYSE, data broker) providing:
```json
{ "symbol": "UBER", "price": 99.01, "closing_price": 85.04 }
```
And users subscribed to stocks — when a stock price jumps/drops X% compared to its closing price, send push notifications to all subscribed users.

---

## 1. Functional Requirements

| # | Requirement |
|---|-------------|
| FR-1 | Users can **register/login** to the platform |
| FR-2 | Users can **subscribe/unsubscribe** to stock symbols (e.g., AAPL, UBER) |
| FR-3 | Users can **configure alert thresholds** (e.g., notify at ±5%, ±10%) — default 5% |
| FR-4 | System **ingests real-time stock price data** from external feed |
| FR-5 | When price deviates ≥ X% from closing price → **send push notification** to all subscribers |
| FR-6 | Users can choose **closing-price baseline** (daily close, weekly close) |
| FR-7 | **No spam** — cooldown after notification sent; don't re-notify until price returns within range |

---

## 2. Non-Functional Requirements

| # | Requirement | Target |
|---|-------------|--------|
| NFR-1 | **Low latency** — notification within **< 2 seconds** of threshold breach |
| NFR-2 | **High availability** — 99.9% uptime |
| NFR-3 | **Scalability** — 10M users, 10K stocks, 100K price updates/sec peak |
| NFR-4 | **At-least-once delivery** — push notifications must not be silently lost |
| NFR-5 | **Idempotent notifications** — no duplicate notifications from duplicate processing |
| NFR-6 | **Fault tolerance** — single component failure doesn't bring down pipeline |
| NFR-7 | **Extensibility** — easy to add SMS, email channels later |

---

## 3. Core Entities

```
User
├── userId (PK)
├── name
├── email
├── deviceToken (FCM/APNs)
└── createdAt

Subscription
├── subscriptionId (PK)
├── userId (FK → User)
├── symbol (e.g., "AAPL")
├── thresholdPct (default 5.0)
├── baselineType (DAILY_CLOSE | WEEKLY_CLOSE)
└── createdAt

Stock
├── symbol (PK, e.g., "UBER")
├── currentPrice
├── dailyClosingPrice
├── weeklyClosingPrice
└── lastUpdatedAt

Notification
├── notificationId (PK)
├── userId (FK → User)
├── symbol
├── triggerPrice
├── closingPrice
├── percentChange
├── sentAt
└── status (SENT | FAILED | PENDING)
```

---

## 4. APIs

### 4.1 User & Subscription Service (REST)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/register` | Register new user (name, email, deviceToken) |
| POST | `/api/v1/users/login` | Authenticate user |
| POST | `/api/v1/subscriptions` | Subscribe `{ symbol, thresholdPct?, baselineType? }` |
| DELETE | `/api/v1/subscriptions/{subscriptionId}` | Unsubscribe |
| GET | `/api/v1/subscriptions?userId={id}` | List user's subscriptions |
| PUT | `/api/v1/subscriptions/{subscriptionId}` | Update threshold / baseline |

### 4.2 Internal / System APIs

| Method | Endpoint / Topic | Description |
|--------|-----------------|-------------|
| — | Kafka topic: `stock-prices` | External feed → ingested price ticks |
| — | Kafka topic: `alerts` | Threshold-breached alerts to be fanned out |
| POST | `/internal/notify` | Notification service sends push via FCM/APNs |

---

## 5. Back-of-the-Envelope Calculations

### Assumptions
| Parameter | Value |
|-----------|-------|
| Total users | 10 M |
| Active stocks | 10 K |
| Avg subscriptions per user | 5 |
| Total subscriptions | 50 M |
| Price updates per second (peak) | 100 K |
| Avg subscribers per stock | 50M / 10K = **5,000** |
| Threshold breach events per day | ~1% of stocks = **100 stocks** |
| Notifications per breach event | 5,000 users |
| Total notifications per day | 100 × 5,000 = **500 K** |
| Peak notifications (burst, market open) | ~50 K in a few seconds |

### Storage
| Data | Size |
|------|------|
| User table (10M rows × 500B) | **~5 GB** |
| Subscription table (50M rows × 200B) | **~10 GB** |
| Stock table (10K rows × 200B) | **~2 MB** (cache-friendly) |
| Notification history (500K/day × 300B × 365 days) | **~55 GB/year** |

### Throughput
| Component | QPS |
|-----------|-----|
| Price ingestion | 100K msg/sec → Kafka handles easily |
| Threshold evaluation | 100K evals/sec (in-memory, sub-ms each) |
| Push notification fan-out (peak) | 50K/sec → horizontally scaled workers |
| Subscription lookups | Hot path → Redis cache (symbol → userIds) |

---

## 6. High-Level Design

### Architecture Diagram

```
┌─────────────────┐
│  External Feed   │  (NASDAQ / NYSE / Data Broker)
│  (WebSocket/API) │
└────────┬────────┘
         │ price ticks
         ▼
┌─────────────────┐       ┌──────────────────────┐
│  Price Ingestion │──────▶│   Kafka              │
│  Service         │       │   topic: stock-prices │
└─────────────────┘       └──────────┬───────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  Alert Evaluator      │
                          │  Service (consumers)  │
                          │                       │
                          │ • Reads price tick     │
                          │ • Compares against     │
                          │   closing price from   │
                          │   Redis/Stock DB       │
                          │ • If |Δ%| ≥ threshold, │
                          │   publishes to alerts  │
                          │   topic                │
                          └──────────┬────────────┘
                                     │
                       ┌─────────────▼────────────┐
                       │  Kafka topic: alerts      │
                       │  { symbol, price, %chg }  │
                       └─────────────┬────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  Notification Fan-out  │
                          │  Service               │
                          │                        │
                          │ • Reads alert           │
                          │ • Looks up subscribers  │
                          │   from Redis/DB         │
                          │ • For each user, pushes │
                          │   to Notification Queue │
                          └──────────┬─────────────┘
                                     │
                       ┌─────────────▼────────────┐
                       │  Notification Queue       │
                       │  (SQS / Kafka)            │
                       └─────────────┬────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  Push Notification     │
                          │  Workers               │
                          │                        │
                          │ • Sends via FCM / APNs │
                          │ • Retries on failure   │
                          │ • Writes to Notif DB   │
                          └────────────────────────┘

  ┌──────────────────────────────────────────────────┐
  │              User-Facing Services                  │
  │                                                    │
  │  ┌──────────────┐    ┌────────────────────┐       │
  │  │ API Gateway   │───▶│ Subscription       │       │
  │  │ (+ Auth)      │    │ Service            │       │
  │  └──────────────┘    │ (CRUD on subs)     │       │
  │                       └───────┬────────────┘       │
  │                               │ writes             │
  │                               ▼                    │
  │                       ┌──────────────┐             │
  │                       │ Subscription │             │
  │                       │ DB (Postgres)│             │
  │                       └──────┬───────┘             │
  │                              │ CDC / sync          │
  │                              ▼                     │
  │                       ┌──────────────┐             │
  │                       │ Redis Cache  │             │
  │                       │ symbol→users │             │
  │                       └──────────────┘             │
  └──────────────────────────────────────────────────┘
```

### Component Breakdown

| Component | Responsibility | Tech Choices |
|-----------|---------------|--------------|
| **Price Ingestion Service** | Connects to external feed via WebSocket; normalizes and publishes to Kafka | Java/Go, Kafka Producer |
| **Kafka (Message Broker)** | Decouples ingestion from evaluation; buffers bursts; enables replay. Topics: `stock-prices`, `alerts`. Partitioned by symbol | Apache Kafka |
| **Alert Evaluator Service** | Consumes price ticks; compares price vs closing price; if breach → publish alert. Maintains cooldown flags in Redis | Java/Go, Kafka Consumer Group |
| **Redis Cache** | (a) Stock closing prices, (b) symbol → subscriber list, (c) cooldown/dedup flags (TTL-based) | Redis Cluster |
| **Notification Fan-out Service** | On alert, looks up all subscribers → enqueues individual notification tasks | Java/Go |
| **Notification Queue** | Buffers individual push notification jobs; retries; absorbs burst | SQS or Kafka |
| **Push Notification Workers** | Sends push via FCM/APNs; handles retries; logs status | Worker pool, FCM/APNs SDK |
| **Subscription Service** | User-facing REST API; writes to Postgres; syncs to Redis via CDC | Spring Boot, Postgres |
| **API Gateway** | Auth, rate limiting, routing | AWS API Gateway / Kong |
| **Stock DB** | Source of truth for closing prices (updated at market close by cron) | Postgres / DynamoDB |
| **Notification DB** | Audit trail of all sent notifications | Postgres / DynamoDB |

### Key Design Decisions

1. **Kafka partitioned by symbol** — all price ticks for same symbol go to same partition → single consumer evaluates threshold (no race conditions on cooldown state)

2. **Two-stage notification pipeline** — Fan-out creates per-user jobs, workers deliver. Separates "who to notify" (fast, in-memory) from "how to deliver" (slow, network I/O)

3. **Cooldown / Dedup** — After alert fires for symbol X, set Redis key `cooldown:X` with TTL (e.g., 15 min). Alert Evaluator skips if cooldown active → prevents spam

4. **CDC (Change Data Capture)** — Subscription changes in Postgres propagated to Redis (via Debezium or app-level sync) keeping `symbol → userIds` mapping fresh

5. **Per-user thresholds** — Alert Evaluator publishes at the *minimum* threshold (e.g., 1%). Fan-out service filters per-user based on their configured threshold. Avoids N evaluations per tick

### Data Flow (Happy Path)

```
1. External feed → Price Ingestion Service → Kafka (stock-prices, partition by symbol)

2. Alert Evaluator picks up tick for UBER, price=$99.01, closing=$85.04
   → Δ% = (99.01 - 85.04) / 85.04 × 100 = 16.4%
   → Exceeds minimum threshold → check cooldown in Redis → no cooldown
   → Publish to Kafka (alerts): { symbol: UBER, price: 99.01, closingPrice: 85.04, pctChange: 16.4 }
   → Set cooldown key in Redis with TTL

3. Fan-out Service reads alert → queries Redis for UBER subscribers (5000 users)
   → For each user, checks user's threshold (e.g., user wants 10%, 16.4 ≥ 10 ✓)
   → Enqueues ~4800 notification jobs (some may not meet personal threshold)

4. Push Workers dequeue jobs → call FCM/APNs → mark SENT in Notification DB

5. User receives: "🚨 UBER is up 16.4% from closing price ($85.04 → $99.01)"
```

### Failure Handling

| Failure | Mitigation |
|---------|------------|
| Price feed goes down | Health check + automatic reconnect; alert ops team |
| Kafka broker failure | Multi-broker cluster with replication factor ≥ 3 |
| Alert Evaluator crash | Kafka consumer group rebalances; another consumer picks up partition |
| FCM/APNs failure | Exponential backoff retry (3 attempts); dead letter queue for manual review |
| Redis cache miss | Fall back to Postgres; lazy-load into Redis |

### Scaling Strategy

| Component | How to Scale |
|-----------|-------------|
| Price Ingestion | Multiple instances, each handling subset of feeds |
| Kafka | Add partitions (one per symbol group) |
| Alert Evaluator | Add consumers to consumer group (max = number of partitions) |
| Fan-out Service | Horizontally scale; each reads from alerts topic |
| Push Workers | Auto-scale based on queue depth (SQS metric) |
| Redis | Redis Cluster with sharding |
| Postgres | Read replicas for subscription lookups; write to primary |
