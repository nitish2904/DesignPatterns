# 🛗 Elevator System — Low Level Design

A complete elevator system implementing **State Pattern**, **Strategy Pattern**, and **Factory Pattern** with a clean layered architecture (Controller → Service → Repository) and **full thread-safety** with a **Producer-Consumer** concurrent architecture.

## Design Patterns Used

| Pattern | Purpose | Classes |
|---------|---------|---------|
| **State** | Elevator behavior changes based on current state (Idle, MovingUp, MovingDown, DoorOpen) | `ElevatorState`, `IdleState`, `MovingUpState`, `MovingDownState`, `DoorOpenState` |
| **Strategy** | Pluggable elevator selection algorithm (Nearest, Least-Loaded) | `ElevatorSelectionStrategy`, `NearestElevatorStrategy`, `LeastLoadedElevatorStrategy` |
| **Factory** | Cached singleton state instances to avoid object creation on every transition | `ElevatorStateFactory`, `StateType` |

## 🔐 Thread-Safety & Concurrency

### Architecture: Producer-Consumer with Thread Pools

```
┌─────────────────────────┐     ┌──────────────────────────┐     ┌─────────────────────────────┐
│  REQUEST PRODUCER POOL  │     │   DISPATCHER THREAD      │     │   ELEVATOR CONSUMER POOL    │
│  (ScheduledThreadPool)  │     │   (SingleThreadExecutor)  │     │   (FixedThreadPool)         │
│                         │     │                          │     │                             │
│  Thread-1: Floor btn    │────▶│  Blocking dequeue from   │────▶│  Thread-E1: Elevator-1 loop │
│  Thread-2: Floor btn    │     │  LinkedBlockingQueue     │     │  Thread-E2: Elevator-2 loop │
│  Thread-3: Cabin btn    │     │  Strategy → select elev  │     │  Thread-E3: Elevator-3 loop │
│                         │     │  Dispatch to elevator    │     │                             │
└─────────────────────────┘     └──────────────────────────┘     └─────────────────────────────┘
```

### Thread-Safety Mechanisms

| Mechanism | Where | Why |
|-----------|-------|-----|
| **`ReentrantLock` (per elevator)** | `Elevator.lock()`/`unlock()` | Fine-grained locking — different elevators operate in parallel |
| **`LinkedBlockingQueue`** | `RequestRepository` | Thread-safe producer-consumer queue with blocking dequeue |
| **`CopyOnWriteArrayList`** | `ElevatorRepository`, `RequestRepository.processedRequests` | Safe concurrent reads (registered once, read many) |
| **`AtomicBoolean`** | `DispatcherService`, `ElevatorRunnerService` | Lock-free lifecycle control |
| **`AtomicInteger`** | `RequestProducerService` | Lock-free request counting |
| **`volatile`** | `DispatcherService.strategy` | Safe strategy swap at runtime across threads |
| **`synchronized`** | `DispatcherService.dispatchNext()` | Atomic dequeue-and-dispatch in sync mode |

### Key Thread-Safety Rule

All state mutations on an Elevator (floor, direction, state, destinations) go through `ElevatorService`, which **acquires the per-elevator lock** before delegating to the State Pattern:

```java
public void handleRequest(Elevator elevator, int src, int dest) {
    elevator.lock();
    try {
        elevator.getState().handleRequest(elevator, src, dest);
    } finally {
        elevator.unlock();
    }
}
```

## 📂 Package Structure

```
Elevator/
├── model/          # Domain entities (Elevator, Request, Direction, RequestType)
├── state/          # State Pattern (ElevatorState + 4 implementations + Factory)
├── strategy/       # Strategy Pattern (selection algorithms)
├── repository/     # Data layer (ElevatorRepository, RequestRepository) — thread-safe
├── service/        # Business logic
│   ├── ElevatorService.java        — CRUD + state delegation with per-elevator locking
│   ├── DispatcherService.java      — Runnable, blocking dequeue + strategy dispatch
│   ├── ElevatorRunnerService.java  — FixedThreadPool (async) + sync movement methods
│   └── RequestProducerService.java — ScheduledThreadPool: concurrent request generation
├── controller/     # Entry points + thread lifecycle orchestration
└── ElevatorMain.java  # Demo: sync + concurrent scenarios
```

## 🔄 How State Pattern Works

1. **`ElevatorService`** is the single entry point for all state interactions — it calls `elevator.getState().handleRequest()` and `elevator.getState().move()`
2. **State objects** decide transitions and call `elevator.setState(ElevatorStateFactory.xxx())` internally
3. **No one outside `ElevatorService`** directly accesses `ElevatorState` — it's an implementation detail

## 📐 UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Direction{
        <<enum>>
        UP
        DOWN
        IDLE
    }
    class RequestType{
        <<enum>>
        EXTERNAL
        INTERNAL
    }
    class StateType{
        <<enum>>
        IDLE
        MOVING_UP
        MOVING_DOWN
        DOOR_OPEN
    }
    class Request{
        -int sourceFloor
        -int destinationFloor
        -Direction direction
        -RequestType type
        -String elevatorId
        +external(src,dest)$ Request
        +internal(id,cur,dest)$ Request
    }
    class Elevator{
        -String id
        -int currentFloor
        -Direction direction
        -ElevatorState state
        -TreeSet destinations
        -int maxCapacity
        -int currentLoad
        -ReentrantLock lock
        +lock()
        +unlock()
        +addDestination(floor)
        +hasDestinations() bool
        +isIdle() bool
    }

    class ElevatorState{
        <<interface>>
        +handleRequest(elev,src,dest)
        +move(elevator)
        +getStateName() String
    }
    class IdleState
    class MovingUpState
    class MovingDownState
    class DoorOpenState

    class ElevatorStateFactory{
        -ElevatorState IDLE$
        -ElevatorState MOVING_UP$
        -ElevatorState MOVING_DOWN$
        -ElevatorState DOOR_OPEN$
        +getState(StateType)$ ElevatorState
        +idle()$ ElevatorState
        +movingUp()$ ElevatorState
        +movingDown()$ ElevatorState
        +doorOpen()$ ElevatorState
    }

    ElevatorState <|.. IdleState : implements
    ElevatorState <|.. MovingUpState : implements
    ElevatorState <|.. MovingDownState : implements
    ElevatorState <|.. DoorOpenState : implements
    ElevatorStateFactory --> StateType : uses
    ElevatorStateFactory --> IdleState : caches
    ElevatorStateFactory --> MovingUpState : caches
    ElevatorStateFactory --> MovingDownState : caches
    ElevatorStateFactory --> DoorOpenState : caches

    class ElevatorSelectionStrategy{
        <<interface>>
        +selectElevator(elevators,req) Elevator
    }
    class NearestElevatorStrategy{
        +selectElevator(elevators,req) Elevator
    }
    class LeastLoadedElevatorStrategy{
        +selectElevator(elevators,req) Elevator
    }
    ElevatorSelectionStrategy <|.. NearestElevatorStrategy : implements
    ElevatorSelectionStrategy <|.. LeastLoadedElevatorStrategy : implements

    class ElevatorRepository{
        -CopyOnWriteArrayList elevators
        +addElevator(e)
        +findById(id) Optional
        +findAll() List
        +findActive() List
    }
    class RequestRepository{
        -LinkedBlockingQueue pendingRequests
        -CopyOnWriteArrayList processedRequests
        +enqueue(req)
        +blockingDequeue(timeout,unit) Request
        +dequeue() Request
        +hasPending() bool
    }

    class RequestService{
        +enqueueExternalRequest(src,dest)
        +enqueueInternalRequest(id,cur,dest)
        +blockingDequeue(timeout,unit) Request
        +hasPendingRequests() bool
        +dequeueNext() Request
    }
    class ElevatorService{
        +registerElevator(e)
        +getElevator(id) Optional
        +getAllElevators() List
        +anyActive() bool
        +handleRequest(elev,src,dest) 🔒
        +move(elevator) 🔒
        +printStatus()
    }
    class DispatcherService{
        <<Runnable>>
        -volatile ElevatorSelectionStrategy strategy
        -AtomicBoolean running
        +run() 🧵
        +stop()
        +setStrategy(s)
        +dispatchNext() synchronized
        +dispatchAll()
    }
    class ElevatorRunnerService{
        -ExecutorService elevatorPool
        -AtomicBoolean running
        -long tickIntervalMs
        +startAll(elevators) 🧵
        +stop()
        +stepAll()
        +processUntilIdle()
    }
    class RequestProducerService{
        -ScheduledExecutorService producerPool
        -ElevatorService elevatorService
        -AtomicInteger produced
        +startProducing(total,interval,n) 🧵
        +shutdown()
    }

    class ExternalRequestController{
        +pressFloorButton(src,dest)
    }
    class InternalRequestController{
        +pressCabinButton(id,cur,dest)
    }
    class ElevatorController{
        -ExecutorService dispatcherExecutor
        +addElevator(e)
        +startElevators() 🧵
        +startDispatcher() 🧵
        +startProducingRequests(n,ms,p) 🧵
        +awaitIdleAndShutdown()
        +shutdown()
        +dispatchAllRequests()
        +processAllMovements()
        +setStrategy(s)
        +printStatus()
    }

    Request --> Direction
    Request --> RequestType
    Elevator --> Direction
    Elevator --> ElevatorState : has current state

    ElevatorRepository --> Elevator : stores
    RequestRepository --> Request : stores

    RequestService --> RequestRepository
    ElevatorService --> ElevatorRepository
    ElevatorService --> ElevatorState : delegates to state
    DispatcherService --> RequestService : blocking dequeue
    DispatcherService --> ElevatorService : calls handleRequest
    DispatcherService --> ElevatorSelectionStrategy : strategy pattern
    ElevatorRunnerService --> ElevatorService : calls move per tick
    RequestProducerService --> RequestService : enqueues requests

    ExternalRequestController --> RequestService
    InternalRequestController --> RequestService
    RequestProducerService --> ElevatorService : reads elevator state

    ElevatorController --> ElevatorService
    ElevatorController --> DispatcherService
    ElevatorController --> ElevatorRunnerService
    ElevatorController --> RequestProducerService
```

## 🚀 How to Run

```bash
# From project root
javac -d out src/Elevator/model/*.java src/Elevator/state/*.java src/Elevator/strategy/*.java src/Elevator/repository/*.java src/Elevator/service/*.java src/Elevator/controller/*.java src/Elevator/ElevatorMain.java
cd out && java Elevator.ElevatorMain
```

## 📋 Demo Scenarios

The `ElevatorMain` runs 3 scenarios:

### Synchronous Mode
1. **External requests** with Nearest Elevator Strategy
2. **Least-Loaded Strategy** — runtime strategy swap

### Concurrent Mode (Producer-Consumer)
3. **Multi-threaded**: 3 producer threads generate 15 random requests at 400ms intervals, 1 dispatcher thread dequeues and dispatches, 3 elevator threads (1 per elevator) process movements in parallel at 300ms per tick
