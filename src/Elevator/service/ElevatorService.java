package Elevator.service;

import Elevator.model.Elevator;
import Elevator.repository.ElevatorRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer: manages elevator entities — registration, lookup, status display.
 *
 * Thread-safety: All state-mutating operations on an elevator are performed
 * while holding that elevator's per-instance ReentrantLock. This ensures
 * that dispatch and movement never collide on the same elevator, while
 * different elevators can operate fully in parallel.
 */
public class ElevatorService {
    private final ElevatorRepository elevatorRepository;

    public ElevatorService(ElevatorRepository elevatorRepository) {
        this.elevatorRepository = elevatorRepository;
    }

    public void registerElevator(Elevator elevator) {
        elevatorRepository.addElevator(elevator);
    }

    public Optional<Elevator> getElevator(String id) {
        return elevatorRepository.findById(id);
    }

    public List<Elevator> getAllElevators() {
        return elevatorRepository.findAll();
    }

    public List<Elevator> getActiveElevators() {
        return elevatorRepository.findActive();
    }

    public boolean anyActive() {
        return !elevatorRepository.findActive().isEmpty();
    }

    // ---- State Pattern delegation (thread-safe) ----

    /**
     * Delegate a request to the elevator's current state.
     * Acquires the per-elevator lock to ensure atomic state transitions.
     */
    public void handleRequest(Elevator elevator, int sourceFloor, int destinationFloor) {
        elevator.lock();
        try {
            elevator.getState().handleRequest(elevator, sourceFloor, destinationFloor);
        } finally {
            elevator.unlock();
        }
    }

    /**
     * Delegate a single movement step to the elevator's current state.
     * Acquires the per-elevator lock to ensure atomic state transitions.
     */
    public void move(Elevator elevator) {
        elevator.lock();
        try {
            elevator.getState().move(elevator);
        } finally {
            elevator.unlock();
        }
    }

    // ---- Status display ----

    public void printStatus() {
        System.out.println("\n========== Elevator Status ==========");
        for (Elevator e : elevatorRepository.findAll()) {
            e.lock();
            try {
                System.out.println("  " + e);
            } finally {
                e.unlock();
            }
        }
        System.out.println("=====================================");
    }
}
