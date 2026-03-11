package Elevator.repository;

import Elevator.model.Request;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * Repository layer: thread-safe in-memory queue for pending and processed requests.
 * Uses LinkedBlockingQueue for producer-consumer pattern.
 */
public class RequestRepository {
    private final BlockingQueue<Request> pendingRequests = new LinkedBlockingQueue<>();
    private final List<Request> processedRequests = new CopyOnWriteArrayList<>();

    /**
     * Non-blocking enqueue — producers call this.
     */
    public void enqueue(Request request) {
        pendingRequests.add(request);
        System.out.println("  [REPO] Enqueued: " + request);
    }

    /**
     * Blocking dequeue — waits until a request is available.
     * Used by the dispatcher thread.
     * @return the request, or null if interrupted/timed out
     */
    public Request blockingDequeue(long timeout, TimeUnit unit) throws InterruptedException {
        Request req = pendingRequests.poll(timeout, unit);
        if (req != null) {
            processedRequests.add(req);
        }
        return req;
    }

    /**
     * Non-blocking dequeue (for backward compat / synchronous use).
     */
    public Request dequeue() {
        Request req = pendingRequests.poll();
        if (req != null) {
            processedRequests.add(req);
        }
        return req;
    }

    public boolean hasPending() {
        return !pendingRequests.isEmpty();
    }

    public int pendingCount() {
        return pendingRequests.size();
    }

    public List<Request> getProcessedRequests() {
        return List.copyOf(processedRequests);
    }
}
