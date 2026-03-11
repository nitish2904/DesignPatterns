package Elevator.service;

import Elevator.model.Elevator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages elevator movement — both synchronous (tick-by-tick) and asynchronous
 * (each elevator in its own thread from a FixedThreadPool).
 *
 * Replaces the old MovementService by consolidating all movement logic here.
 *
 * In async mode: Elevators are consumers in a Producer-Consumer architecture —
 * they consume destinations dispatched by the DispatcherService.
 */
public class ElevatorRunnerService {
    private final ElevatorService elevatorService;
    private ExecutorService elevatorPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long tickIntervalMs;

    public ElevatorRunnerService(ElevatorService elevatorService, long tickIntervalMs) {
        this.elevatorService = elevatorService;
        this.tickIntervalMs = tickIntervalMs;
    }

    // ===== ASYNC MODE: Thread pools =====

    /**
     * Start a dedicated thread for each elevator. Each thread loops:
     *   1. If elevator has destinations → move one step
     *   2. Sleep for tickIntervalMs (simulate floor travel time)
     */
    public void startAll(List<Elevator> elevators) {
        elevatorPool = Executors.newFixedThreadPool(elevators.size(), r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        running.set(true);

        for (Elevator elevator : elevators) {
            elevatorPool.submit(() -> runElevator(elevator));
        }

        System.out.println("  [ElevatorRunner] Started " + elevators.size()
                + " elevator threads (tick=" + tickIntervalMs + "ms)");
    }

    private void runElevator(Elevator elevator) {
        String threadName = "Elev-" + elevator.getId();
        Thread.currentThread().setName(threadName);
        System.out.println("  [" + elevator.getId() + "] Running on thread: " + threadName);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (elevator.hasDestinations()) {
                    elevatorService.move(elevator);  // acquires per-elevator lock internally
                }
                Thread.sleep(tickIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("  [" + elevator.getId() + "] Thread stopped.");
    }

    /**
     * Stop all elevator threads.
     */
    public void stop() {
        running.set(false);
        if (elevatorPool != null) {
            elevatorPool.shutdownNow();
        }
    }

    /**
     * Wait for all elevator threads to finish (after stop() is called).
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (elevatorPool != null) {
            return elevatorPool.awaitTermination(timeout, unit);
        }
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    // ===== SYNC MODE: Backward-compatible synchronous movement =====

    /**
     * Move a single elevator one step synchronously (thread-safe via ElevatorService locking).
     */
    public void stepElevator(Elevator elevator) {
        elevatorService.move(elevator);
    }

    /**
     * Move all active elevators one step synchronously.
     */
    public void stepAll() {
        for (Elevator elevator : elevatorService.getAllElevators()) {
            if (elevator.hasDestinations()) {
                stepElevator(elevator);
            }
        }
    }

    /**
     * Run all elevators synchronously until every pending stop is served.
     */
    public void processUntilIdle() {
        System.out.println("\n--- Processing all pending movements ---");
        int tick = 0;
        while (elevatorService.anyActive()) {
            tick++;
            System.out.println("\n[Tick " + tick + "]");
            List<Elevator> active = elevatorService.getActiveElevators();
            for (Elevator elevator : active) {
                stepElevator(elevator);
            }
        }
        System.out.println("\n--- All movements processed ---");
    }
}
