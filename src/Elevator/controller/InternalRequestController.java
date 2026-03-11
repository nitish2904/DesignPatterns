package Elevator.controller;

import Elevator.service.RequestService;

/**
 * Controller: handles requests from inside an elevator cabin (INTERNAL).
 * A passenger inside elevator presses a destination floor button.
 */
public class InternalRequestController {
    private final RequestService requestService;

    public InternalRequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Passenger inside elevator {@code elevatorId} presses button for {@code destinationFloor}.
     */
    public void pressCabinButton(String elevatorId, int currentFloor, int destinationFloor) {
        System.out.println("\n[InternalController] Cabin button pressed in " + elevatorId
                + ": floor " + currentFloor + " → floor " + destinationFloor);
        requestService.enqueueInternalRequest(elevatorId, currentFloor, destinationFloor);
    }
}
