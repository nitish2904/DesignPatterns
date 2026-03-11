package Elevator.controller;

import Elevator.model.Elevator;
import Elevator.service.DispatcherService;
import Elevator.service.ElevatorRunnerService;
import Elevator.service.ElevatorService;
import Elevator.service.RequestProducerService;
import Elevator.strategy.ElevatorSelectionStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller: top-level orchestrator for the elevator system.
 * Manages thread lifecycle for producers, dispatcher, and elevator runners.
 *
 * Thread architecture:
 *   • N producer threads  (ScheduledThreadPool)  — generate external + internal requests
 *   • 1 dispatcher thread (SingleThreadExecutor)  — dequeue + dispatch
 *   • M elevator threads  (FixedThreadPool)       — move elevators
 */
public class ElevatorController {
    private final ElevatorService elevatorService;
    private final DispatcherService dispatcherService;
    private final ElevatorRunnerService elevatorRunnerService;
    private final RequestProducerService requestProducerService;

    private ExecutorService dispatcherExecutor;

    public ElevatorController(ElevatorService elevatorService,
                              DispatcherService dispatcherService,
                              ElevatorRunnerService elevatorRunnerService,
                              RequestProducerService requestProducerService) {
        this.elevatorService = elevatorService;
        this.dispatcherService = dispatcherService;
        this.elevatorRunnerService = elevatorRunnerService;
        this.requestProducerService = requestProducerService;
    }

    /**
     * Register a new elevator in the system.
     */
    public void addElevator(Elevator elevator) {
        elevatorService.registerElevator(elevator);
    }

    // ===== ASYNC MODE: Thread pools =====

    /**
     * Start elevator consumer threads (1 per elevator).
     */
    public void startElevators() {
        elevatorRunnerService.startAll(elevatorService.getAllElevators());
    }

    /**
     * Start the dispatcher thread — blocks on the request queue.
     */
    public void startDispatcher() {
        dispatcherExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Dispatcher-Thread");
            t.setDaemon(true);
            return t;
        });
        dispatcherExecutor.submit(dispatcherService);
    }

    /**
     * Start producing random requests via the producer thread pool.
     */
    public void startProducingRequests(int totalRequests, long intervalMs, int numProducers) {
        requestProducerService.startProducing(totalRequests, intervalMs, numProducers);
    }

    /**
     * Gracefully wait for elevators to go idle, then shut down.
     */
    public void awaitIdleAndShutdown(long pollIntervalMs, long maxWaitMs) {
        System.out.println("\n⏳ Waiting for all elevators to become idle...");
        long waited = 0;
        while (waited < maxWaitMs) {
            if (!elevatorService.anyActive()) {
                try { Thread.sleep(pollIntervalMs * 2); } catch (InterruptedException e) { break; }
                if (!elevatorService.anyActive()) {
                    System.out.println("  ✅ All elevators idle.");
                    break;
                }
            }
            try { Thread.sleep(pollIntervalMs); } catch (InterruptedException e) { break; }
            waited += pollIntervalMs;
        }
        shutdown();
    }

    /**
     * Force shutdown all threads.
     */
    public void shutdown() {
        System.out.println("\n🛑 Shutting down...");
        requestProducerService.shutdown();
        dispatcherService.stop();
        elevatorRunnerService.stop();

        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }

        System.out.println("  All threads shut down.");
    }

    // ===== SYNC MODE: Backward-compatible synchronous operations =====

    public void dispatchAllRequests() {
        dispatcherService.dispatchAll();
    }

    public void stepAll() {
        elevatorRunnerService.stepAll();
    }

    public void processAllMovements() {
        elevatorRunnerService.processUntilIdle();
    }

    public void setStrategy(ElevatorSelectionStrategy strategy) {
        dispatcherService.setStrategy(strategy);
    }

    public void printStatus() {
        elevatorService.printStatus();
    }
}
