package Elevator.state;

import Elevator.model.Elevator;

/**
 * State interface for the State Pattern.
 * Each elevator state (Idle, MovingUp, MovingDown, DoorOpen) implements this.
 */
public interface ElevatorState {

    /**
     * Handle adding destinations to the elevator while in this state.
     */
    void handleRequest(Elevator elevator, int sourceFloor, int destinationFloor);

    /**
     * Move the elevator one step (simulate one tick).
     */
    void move(Elevator elevator);

    /**
     * Get the display name of this state.
     */
    String getStateName();
}
