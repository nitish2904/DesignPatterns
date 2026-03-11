package Elevator.repository;

import Elevator.model.Elevator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository layer: thread-safe in-memory store for all Elevator entities.
 * Uses CopyOnWriteArrayList — elevators are registered once, read many times.
 */
public class ElevatorRepository {
    private final CopyOnWriteArrayList<Elevator> elevators = new CopyOnWriteArrayList<>();

    public void addElevator(Elevator elevator) {
        elevators.add(elevator);
        System.out.println("  [REPO] Registered elevator: " + elevator.getId());
    }

    public Optional<Elevator> findById(String id) {
        return elevators.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    public List<Elevator> findAll() {
        return Collections.unmodifiableList(elevators);
    }

    public List<Elevator> findIdle() {
        return elevators.stream()
                .filter(Elevator::isIdle)
                .toList();
    }

    public List<Elevator> findActive() {
        return elevators.stream()
                .filter(Elevator::hasDestinations)
                .toList();
    }

    public int count() {
        return elevators.size();
    }
}
