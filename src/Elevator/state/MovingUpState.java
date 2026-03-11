package Elevator.state;

import Elevator.model.Direction;
import Elevator.model.Elevator;

/**
 * Elevator is moving upward.
 */
public class MovingUpState implements ElevatorState {

    @Override
    public void handleRequest(Elevator elevator, int sourceFloor, int destinationFloor) {
        elevator.addDestination(sourceFloor);
        elevator.addDestination(destinationFloor);
        System.out.println("  [" + elevator.getId() + "] Queued stops (" + sourceFloor
                + ", " + destinationFloor + ") while moving UP");
    }

    @Override
    public void move(Elevator elevator) {
        if (!elevator.hasDestinations()) {
            elevator.setDirection(Direction.IDLE);
            elevator.setState(ElevatorStateFactory.idle());
            System.out.println("  [" + elevator.getId() + "] No more destinations. Now IDLE at floor "
                    + elevator.getCurrentFloor());
            return;
        }

        elevator.setCurrentFloor(elevator.getCurrentFloor() + 1);
        System.out.println("  [" + elevator.getId() + "] Moved UP to floor " + elevator.getCurrentFloor());

        if (elevator.getDestinations().contains(elevator.getCurrentFloor())) {
            elevator.getDestinations().remove(elevator.getCurrentFloor());
            elevator.setState(ElevatorStateFactory.doorOpen());
            elevator.getState().move(elevator);
        }

        if (!elevator.hasDestinations()) {
            elevator.setDirection(Direction.IDLE);
            elevator.setState(ElevatorStateFactory.idle());
            System.out.println("  [" + elevator.getId() + "] All stops served. Now IDLE at floor "
                    + elevator.getCurrentFloor());
        } else if (elevator.getDestinations().stream().allMatch(f -> f < elevator.getCurrentFloor())) {
            elevator.setDirection(Direction.DOWN);
            elevator.setState(ElevatorStateFactory.movingDown());
            System.out.println("  [" + elevator.getId() + "] Switching direction to DOWN");
        }
    }

    @Override
    public String getStateName() {
        return "MOVING_UP";
    }
}
