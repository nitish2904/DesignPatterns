package Elevator.strategy;

import Elevator.model.Elevator;
import Elevator.model.Request;

import java.util.List;

/**
 * Selects the elevator with the fewest pending stops (least loaded).
 * Ties are broken by distance to the source floor.
 */
public class LeastLoadedElevatorStrategy implements ElevatorSelectionStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int bestPending = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            int pending = elevator.getDestinations().size();
            int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());

            if (pending < bestPending || (pending == bestPending && distance < bestDistance)) {
                bestPending = pending;
                bestDistance = distance;
                best = elevator;
            }
        }

        System.out.println("  >> LeastLoadedStrategy picked " + (best != null ? best.getId() : "NONE")
                + " (pending=" + bestPending + ", dist=" + bestDistance + ") for " + request);
        return best;
    }
}
