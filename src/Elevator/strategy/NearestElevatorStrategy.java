package Elevator.strategy;

import Elevator.model.Direction;
import Elevator.model.Elevator;
import Elevator.model.Request;

import java.util.List;

/**
 * Selects the nearest elevator that is either idle or already moving in the
 * direction of the request source floor.
 */
public class NearestElevatorStrategy implements ElevatorSelectionStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
            int score;

            if (elevator.isIdle()) {
                score = distance;
            } else if (isMovingToward(elevator, request.getSourceFloor())) {
                score = distance;
            } else {
                score = distance + 1000; // penalise — moving away
            }

            if (score < bestScore) {
                bestScore = score;
                best = elevator;
            }
        }

        System.out.println("  >> NearestStrategy picked " + (best != null ? best.getId() : "NONE")
                + " (score=" + bestScore + ") for " + request);
        return best;
    }

    private boolean isMovingToward(Elevator elevator, int targetFloor) {
        if (elevator.getDirection() == Direction.UP) {
            return targetFloor >= elevator.getCurrentFloor();
        } else if (elevator.getDirection() == Direction.DOWN) {
            return targetFloor <= elevator.getCurrentFloor();
        }
        return true;
    }
}
