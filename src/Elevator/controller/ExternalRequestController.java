package Elevator.controller;

import Elevator.service.RequestService;

/**
 * Controller: handles requests from floor hall-call buttons (EXTERNAL).
 * A person on a floor presses UP/DOWN, then selects a destination.
 */
public class ExternalRequestController {
    private final RequestService requestService;

    public ExternalRequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Person on {@code sourceFloor} requests to go to {@code destinationFloor}.
     */
    public void pressFloorButton(int sourceFloor, int destinationFloor) {
        System.out.println("\n[ExternalController] Floor button pressed: floor " + sourceFloor
                + " → floor " + destinationFloor);
        requestService.enqueueExternalRequest(sourceFloor, destinationFloor);
    }
}
