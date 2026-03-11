package Elevator.service;

import Elevator.model.Elevator;
import Elevator.model.Request;
import Elevator.model.RequestType;
import Elevator.strategy.ElevatorSelectionStrategy;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service layer: dequeues requests from the blocking queue, selects the best
 * elevator using the Strategy Pattern, and dispatches requests.
 *
 * Implements Runnable so it can run in its own thread, blocking on the queue
 * until requests arrive (producer-consumer pattern).
 */
public class DispatcherService implements Runnable {
    private final RequestService requestService;
    private final ElevatorService elevatorService;
    private volatile ElevatorSelectionStrategy strategy;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DispatcherService(RequestService requestService,
                             ElevatorService elevatorService,
                             ElevatorSelectionStrategy strategy) {
        this.requestService = requestService;
        this.elevatorService = elevatorService;
        this.strategy = strategy;
    }

    /**
     * Change elevator selection strategy at runtime (thread-safe via volatile).
     */
    public void setStrategy(ElevatorSelectionStrategy strategy) {
        this.strategy = strategy;
        System.out.println("\n✦ Dispatcher strategy changed to: " + strategy.getClass().getSimpleName());
    }

    /**
     * Main loop — runs in its own thread. Blocks waiting for requests,
     * then dispatches each one to the best elevator.
     */
    @Override
    public void run() {
        running.set(true);
        System.out.println("  [Dispatcher] Started on thread: " + Thread.currentThread().getName());
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Request request = requestService.blockingDequeue(500, TimeUnit.MILLISECONDS);
                if (request != null) {
                    dispatch(request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("  [Dispatcher] Stopped.");
    }

    /**
     * Stop the dispatcher loop.
     */
    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Synchronous dispatch of a single request (for non-threaded use / backward compat).
     */
    public synchronized void dispatchNext() {
        if (!requestService.hasPendingRequests()) {
            System.out.println("  [Dispatcher] No pending requests to dispatch.");
            return;
        }
        Request request = requestService.dequeueNext();
        if (request != null) {
            System.out.println("\n→ Dispatching: " + request);
            dispatch(request);
        }
    }

    /**
     * Dequeue and dispatch ALL pending requests synchronously.
     */
    public void dispatchAll() {
        System.out.println("\n--- Dispatching all pending requests (" + requestService.pendingCount() + ") ---");
        while (requestService.hasPendingRequests()) {
            dispatchNext();
        }
        System.out.println("--- All requests dispatched ---");
    }

    // ---- Private helpers ----

    private void dispatch(Request request) {
        System.out.println("  [Dispatcher] Dispatching " + request + " [" + Thread.currentThread().getName() + "]");

        if (request.getType() == RequestType.INTERNAL) {
            dispatchInternal(request);
        } else {
            dispatchExternal(request);
        }
    }

    private void dispatchExternal(Request request) {
        Elevator selected = strategy.selectElevator(
                elevatorService.getAllElevators(), request);

        if (selected != null) {
            elevatorService.handleRequest(selected,
                    request.getSourceFloor(), request.getDestinationFloor());
        } else {
            System.out.println("  !! No elevator available for " + request);
        }
    }

    private void dispatchInternal(Request request) {
        Optional<Elevator> opt = elevatorService.getElevator(request.getElevatorId());
        if (opt.isPresent()) {
            Elevator elevator = opt.get();
            elevatorService.handleRequest(elevator,
                    request.getSourceFloor(), request.getDestinationFloor());
        } else {
            System.out.println("  !! Elevator " + request.getElevatorId() + " not found for " + request);
        }
    }
}
