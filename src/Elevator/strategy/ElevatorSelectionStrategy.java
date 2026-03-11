package Elevator.strategy;

import Elevator.model.Elevator;
import Elevator.model.Request;

import java.util.List;

/**
 * Strategy interface for selecting the best elevator to serve a request.
 * Different algorithms can be plugged in (Nearest, Least-Loaded, etc.).
 */
public interface ElevatorSelectionStrategy {

    /**
     * Select the most appropriate elevator from the list to handle the given request.
     */
    Elevator selectElevator(List<Elevator> elevators, Request request);
}
