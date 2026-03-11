package Elevator.model;

import Elevator.state.ElevatorState;
import Elevator.state.ElevatorStateFactory;
import Elevator.state.IdleState;

import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core Elevator entity — context for the State Pattern.
 * Holds floor, direction, state and a sorted set of destination stops.
 *
 * Thread-safety: Each elevator has its own ReentrantLock.
 * All state mutations (floor, direction, state, destinations) must be done
 * while holding this lock. ElevatorService is responsible for acquiring it.
 */
public class Elevator {
    private final String id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;
    private final TreeSet<Integer> destinations; // sorted pending stops
    private final int maxCapacity;
    private int currentLoad;

    /** Per-elevator lock for thread-safe state mutations. */
    private final ReentrantLock lock = new ReentrantLock();

    public Elevator(String id, int startFloor, int maxCapacity) {
        this.id = id;
        this.currentFloor = startFloor;
        this.direction = Direction.IDLE;
        this.state = ElevatorStateFactory.idle();
        this.destinations = new TreeSet<>();
        this.maxCapacity = maxCapacity;
        this.currentLoad = 0;
    }

    // ---- Locking ----

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    // ---- Destination management ----

    public void addDestination(int floor) {
        if (floor != currentFloor) {
            destinations.add(floor);
        }
    }

    public TreeSet<Integer> getDestinations() {
        return destinations;
    }

    public boolean hasDestinations() {
        return !destinations.isEmpty();
    }

    // ---- Getters / Setters ----

    public String getId() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public ElevatorState getState() {
        return state;
    }

    public void setState(ElevatorState state) {
        this.state = state;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public boolean isIdle() {
        return state instanceof IdleState;
    }

    @Override
    public String toString() {
        return "Elevator{" + id
                + ", floor=" + currentFloor
                + ", dir=" + direction
                + ", state=" + state.getStateName()
                + ", stops=" + destinations
                + ", load=" + currentLoad + "/" + maxCapacity
                + "}";
    }
}
