package Elevator.state;

import Elevator.model.Direction;
import Elevator.model.Elevator;

/**
 * Elevator is idle — not moving, doors closed, waiting for requests.
 */
public class IdleState implements ElevatorState {

    @Override
    public void handleRequest(Elevator elevator, int sourceFloor, int destinationFloor) {
        elevator.addDestination(sourceFloor);
        elevator.addDestination(destinationFloor);

        if (sourceFloor > elevator.getCurrentFloor()) {
            elevator.setDirection(Direction.UP);
            elevator.setState(ElevatorStateFactory.movingUp());
        } else if (sourceFloor < elevator.getCurrentFloor()) {
            elevator.setDirection(Direction.DOWN);
            elevator.setState(ElevatorStateFactory.movingDown());
        } else {
            // Already at the source floor — open doors, then pick direction
            elevator.setState(ElevatorStateFactory.doorOpen());
            elevator.getState().move(elevator);
        }

        System.out.println("  [" + elevator.getId() + "] Accepted request (src=" + sourceFloor
                + ", dest=" + destinationFloor + ") | Now " + elevator.getState().getStateName());
    }

    @Override
    public void move(Elevator elevator) {
        // Idle — nothing to do
    }

    @Override
    public String getStateName() {
        return "IDLE";
    }
}
