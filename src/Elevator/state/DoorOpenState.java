package Elevator.state;

import Elevator.model.Direction;
import Elevator.model.Elevator;

/**
 * Elevator doors are open — passengers boarding/alighting.
 */
public class DoorOpenState implements ElevatorState {

    @Override
    public void handleRequest(Elevator elevator, int sourceFloor, int destinationFloor) {
        elevator.addDestination(sourceFloor);
        elevator.addDestination(destinationFloor);
        System.out.println("  [" + elevator.getId() + "] Queued stops (" + sourceFloor
                + ", " + destinationFloor + ") while doors open");
    }

    @Override
    public void move(Elevator elevator) {
        System.out.println("  [" + elevator.getId() + "] Doors OPEN at floor " + elevator.getCurrentFloor());
        System.out.println("  [" + elevator.getId() + "] Doors CLOSING at floor " + elevator.getCurrentFloor());

        if (!elevator.hasDestinations()) {
            elevator.setDirection(Direction.IDLE);
            elevator.setState(ElevatorStateFactory.idle());
            System.out.println("  [" + elevator.getId() + "] No pending requests. Now IDLE");
        } else if (elevator.getDirection() == Direction.UP) {
            boolean hasUpStops = elevator.getDestinations().stream()
                    .anyMatch(f -> f > elevator.getCurrentFloor());
            if (hasUpStops) {
                elevator.setState(ElevatorStateFactory.movingUp());
            } else {
                elevator.setDirection(Direction.DOWN);
                elevator.setState(ElevatorStateFactory.movingDown());
            }
        } else if (elevator.getDirection() == Direction.DOWN) {
            boolean hasDownStops = elevator.getDestinations().stream()
                    .anyMatch(f -> f < elevator.getCurrentFloor());
            if (hasDownStops) {
                elevator.setState(ElevatorStateFactory.movingDown());
            } else {
                elevator.setDirection(Direction.UP);
                elevator.setState(ElevatorStateFactory.movingUp());
            }
        } else {
            int next = elevator.getDestinations().first();
            if (next > elevator.getCurrentFloor()) {
                elevator.setDirection(Direction.UP);
                elevator.setState(ElevatorStateFactory.movingUp());
            } else {
                elevator.setDirection(Direction.DOWN);
                elevator.setState(ElevatorStateFactory.movingDown());
            }
        }
    }

    @Override
    public String getStateName() {
        return "DOOR_OPEN";
    }
}
