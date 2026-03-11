package Elevator.service;

import Elevator.model.Request;
import Elevator.repository.RequestRepository;

import java.util.concurrent.TimeUnit;

/**
 * Service layer: validates and enqueues requests into the RequestRepository.
 * Both external and internal controllers delegate here.
 * Thread-safe: delegates to LinkedBlockingQueue in the repository.
 */
public class RequestService {
    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Validate and enqueue an external request (floor hall-call).
     */
    public void enqueueExternalRequest(int sourceFloor, int destinationFloor) {
        if (sourceFloor == destinationFloor) {
            System.out.println("  [RequestService] Ignored — source == destination (floor " + sourceFloor + ")");
            return;
        }
        Request request = Request.external(sourceFloor, destinationFloor);
        System.out.println("  [RequestService] External request created: " + request
                + " [" + Thread.currentThread().getName() + "]");
        requestRepository.enqueue(request);
    }

    /**
     * Validate and enqueue an internal request (cabin button press).
     */
    public void enqueueInternalRequest(String elevatorId, int currentFloor, int destinationFloor) {
        if (currentFloor == destinationFloor) {
            System.out.println("  [RequestService] Ignored — already at floor " + currentFloor);
            return;
        }
        Request request = Request.internal(elevatorId, currentFloor, destinationFloor);
        System.out.println("  [RequestService] Internal request created: " + request
                + " [" + Thread.currentThread().getName() + "]");
        requestRepository.enqueue(request);
    }

    /**
     * Blocking dequeue — waits up to the given timeout for a request.
     * Used by the DispatcherService running in its own thread.
     */
    public Request blockingDequeue(long timeout, TimeUnit unit) throws InterruptedException {
        return requestRepository.blockingDequeue(timeout, unit);
    }

    public boolean hasPendingRequests() {
        return requestRepository.hasPending();
    }

    public Request dequeueNext() {
        return requestRepository.dequeue();
    }

    public int pendingCount() {
        return requestRepository.pendingCount();
    }
}
