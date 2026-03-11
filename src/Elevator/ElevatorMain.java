package Elevator;

import Elevator.controller.ElevatorController;
import Elevator.controller.ExternalRequestController;
import Elevator.controller.InternalRequestController;
import Elevator.model.Elevator;
import Elevator.repository.ElevatorRepository;
import Elevator.repository.RequestRepository;
import Elevator.service.*;
import Elevator.strategy.ElevatorSelectionStrategy;
import Elevator.strategy.LeastLoadedElevatorStrategy;
import Elevator.strategy.NearestElevatorStrategy;

/**
 * =====================================================================
 *  Elevator System — Low Level Design (Thread-Safe + Concurrent)
 * =====================================================================
 *
 *  Architecture (Layered + Producer-Consumer):
 *
 *    PRODUCER THREADS (ScheduledThreadPool)
 *      → Generate random external + internal requests concurrently
 *      → Enqueue into LinkedBlockingQueue
 *
 *    DISPATCHER THREAD (SingleThreadExecutor)
 *      → Blocking dequeue from request queue
 *      → Strategy Pattern selects best elevator
 *      → Dispatches request to elevator (per-elevator lock)
 *
 *    ELEVATOR THREADS (FixedThreadPool, 1 per elevator)
 *      → Each elevator runs in its own thread
 *      → Continuously moves one floor per tick
 *      → State Pattern manages transitions
 *
 *  Thread-Safety:
 *    • Per-elevator ReentrantLock (fine-grained, no global lock)
 *    • LinkedBlockingQueue for request queue
 *    • CopyOnWriteArrayList for elevator registry
 *    • AtomicBoolean for lifecycle control
 *    • volatile for strategy swap
 *
 *  Design Patterns:
 *    1. STATE PATTERN    — Elevator state (Idle, MovingUp, MovingDown, DoorOpen)
 *    2. STRATEGY PATTERN — Elevator selection (Nearest, LeastLoaded)
 *    3. FACTORY PATTERN  — Cached singleton state instances
 * =====================================================================
 */
public class ElevatorMain {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   Elevator System — Concurrent LLD Demo              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // ===== WIRING =====

        // -- Repository layer --
        ElevatorRepository elevatorRepo = new ElevatorRepository();
        RequestRepository requestRepo = new RequestRepository();

        // -- Service layer --
        ElevatorService elevatorService = new ElevatorService(elevatorRepo);
        RequestService requestService = new RequestService(requestRepo);

        ElevatorSelectionStrategy nearestStrategy = new NearestElevatorStrategy();
        DispatcherService dispatcherService = new DispatcherService(
                requestService, elevatorService, nearestStrategy);

        // Elevator runner: each elevator in its own thread, 300ms per tick
        ElevatorRunnerService elevatorRunner = new ElevatorRunnerService(elevatorService, 300);

        // Request producer: generates both external + internal requests
        RequestProducerService requestProducer = new RequestProducerService(
                requestService, elevatorService, 20);

        // -- Controller layer --
        ExternalRequestController externalController = new ExternalRequestController(requestService);
        InternalRequestController internalController = new InternalRequestController(requestService);
        ElevatorController elevatorController = new ElevatorController(
                elevatorService, dispatcherService, elevatorRunner, requestProducer);

        // ===== SETUP =====

        System.out.println("\n--- Registering Elevators ---");
        elevatorController.addElevator(new Elevator("E1", 1, 10));
        elevatorController.addElevator(new Elevator("E2", 5, 10));
        elevatorController.addElevator(new Elevator("E3", 10, 10));

        elevatorController.printStatus();

        // =========================================================
        // SCENARIO 1: Synchronous Mode (backward compatible)
        // =========================================================
        System.out.println("\n\n" + "=".repeat(60));
        System.out.println("  SCENARIO 1: Synchronous — External + Internal Requests");
        System.out.println("=".repeat(60));

        // External requests (floor hall-call buttons)
        externalController.pressFloorButton(3, 9);
        externalController.pressFloorButton(7, 2);
        externalController.pressFloorButton(12, 1);

        // Internal request (cabin button — passenger inside E2 presses floor 18)
        int e2Floor = elevatorService.getElevator("E2").get().getCurrentFloor();
        internalController.pressCabinButton("E2", e2Floor, 18);

        elevatorController.dispatchAllRequests();
        elevatorController.processAllMovements();
        elevatorController.printStatus();

        // =========================================================
        // SCENARIO 2: Synchronous — Least-Loaded Strategy
        // =========================================================
        System.out.println("\n\n" + "=".repeat(60));
        System.out.println("  SCENARIO 2: Synchronous — Least-Loaded Strategy");
        System.out.println("=".repeat(60));

        elevatorController.setStrategy(new LeastLoadedElevatorStrategy());

        externalController.pressFloorButton(1, 15);
        internalController.pressCabinButton("E3",
                elevatorService.getElevator("E3").get().getCurrentFloor(), 3);
        externalController.pressFloorButton(14, 6);

        elevatorController.dispatchAllRequests();
        elevatorController.processAllMovements();
        elevatorController.printStatus();

        // =========================================================
        // SCENARIO 3: CONCURRENT MODE — Producer-Consumer
        // =========================================================
        System.out.println("\n\n" + "=".repeat(60));
        System.out.println("  SCENARIO 3: CONCURRENT MODE — Producer-Consumer");
        System.out.println("  Producers: 3 threads generating 15 requests");
        System.out.println("    (~70% external floor buttons, ~30% internal cabin buttons)");
        System.out.println("  Dispatcher: 1 thread (blocking dequeue + strategy)");
        System.out.println("  Elevators: 3 threads (1 per elevator, parallel movement)");
        System.out.println("=".repeat(60));

        // Reset strategy
        elevatorController.setStrategy(new NearestElevatorStrategy());

        // Start elevator threads (consumers)
        elevatorController.startElevators();

        // Start dispatcher thread
        elevatorController.startDispatcher();

        // Start producers: 15 requests, 3 producer threads, 400ms interval
        // Generates BOTH external + internal requests randomly
        elevatorController.startProducingRequests(15, 400, 3);

        // Let the system run for a while
        System.out.println("\n⏳ Letting concurrent system run for 12 seconds...\n");
        Thread.sleep(12000);

        elevatorController.printStatus();

        // Wait for elevators to go idle, then shut down
        elevatorController.awaitIdleAndShutdown(500, 10000);

        elevatorController.printStatus();

        System.out.println("\n✅ Demo complete.");
    }
}
