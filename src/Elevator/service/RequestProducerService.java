package Elevator.service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates concurrent request generation using a ScheduledExecutorService.
 *
 * Producer-Consumer: This is the PRODUCER — it generates both external
 * (floor hall-call) and internal (cabin button) requests at configurable
 * intervals and enqueues them via RequestService.
 */
public class RequestProducerService {
    private final RequestService requestService;
    private final ElevatorService elevatorService;
    private final int totalFloors;
    private final Random random = new Random();
    private ScheduledExecutorService producerPool;

    public RequestProducerService(RequestService requestService,
                                  ElevatorService elevatorService,
                                  int totalFloors) {
        this.requestService = requestService;
        this.elevatorService = elevatorService;
        this.totalFloors = totalFloors;
    }

    /**
     * Start producing random external AND internal requests.
     *
     * ~70% external requests (floor hall-call buttons)
     * ~30% internal requests (cabin panel buttons from a random elevator)
     *
     * @param totalRequests    how many requests to produce in total
     * @param intervalMs       interval between requests (per producer thread)
     * @param numProducers     number of concurrent producer threads
     */
    public void startProducing(int totalRequests, long intervalMs, int numProducers) {
        AtomicInteger produced = new AtomicInteger(0);

        producerPool = Executors.newScheduledThreadPool(numProducers, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Producer-" + t.getId());
            return t;
        });

        for (int i = 0; i < numProducers; i++) {
            final int producerId = i + 1;
            producerPool.scheduleAtFixedRate(() -> {
                if (produced.get() >= totalRequests) {
                    return;
                }

                int count = produced.incrementAndGet();
                if (count > totalRequests) return;

                boolean isInternal = random.nextInt(10) < 3; // 30% chance internal

                if (isInternal) {
                    generateInternalRequest(producerId, count);
                } else {
                    generateExternalRequest(producerId, count);
                }

                if (count >= totalRequests) {
                    System.out.println("  [Producer] All " + totalRequests + " requests produced.");
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
        }

        System.out.println("  [RequestProducer] Started " + numProducers
                + " producer threads, target=" + totalRequests
                + " requests (~70% external, ~30% internal), interval=" + intervalMs + "ms");
    }

    private void generateExternalRequest(int producerId, int count) {
        int src = random.nextInt(totalFloors) + 1;
        int dest = random.nextInt(totalFloors) + 1;
        while (dest == src) {
            dest = random.nextInt(totalFloors) + 1;
        }

        System.out.println("  [Producer-" + producerId + "] Request #" + count
                + " EXTERNAL: floor " + src + " → " + dest
                + " [" + Thread.currentThread().getName() + "]");
        requestService.enqueueExternalRequest(src, dest);
    }

    private void generateInternalRequest(int producerId, int count) {
        // Pick a random elevator and simulate a cabin button press
        var elevators = elevatorService.getAllElevators();
        if (elevators.isEmpty()) return;

        var elevator = elevators.get(random.nextInt(elevators.size()));
        int currentFloor = elevator.getCurrentFloor();
        int dest = random.nextInt(totalFloors) + 1;
        while (dest == currentFloor) {
            dest = random.nextInt(totalFloors) + 1;
        }

        System.out.println("  [Producer-" + producerId + "] Request #" + count
                + " INTERNAL: " + elevator.getId() + " (floor " + currentFloor + ") → " + dest
                + " [" + Thread.currentThread().getName() + "]");
        requestService.enqueueInternalRequest(elevator.getId(), currentFloor, dest);
    }

    public void shutdown() {
        if (producerPool != null) {
            producerPool.shutdownNow();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (producerPool != null) {
            return producerPool.awaitTermination(timeout, unit);
        }
        return true;
    }
}
