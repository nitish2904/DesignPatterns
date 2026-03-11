package Elevator.state;

/**
 * Factory for ElevatorState objects.
 * Since all states are stateless, we cache singleton instances
 * to avoid creating new objects on every state transition.
 */
public class ElevatorStateFactory {

    private static final ElevatorState IDLE = new IdleState();
    private static final ElevatorState MOVING_UP = new MovingUpState();
    private static final ElevatorState MOVING_DOWN = new MovingDownState();
    private static final ElevatorState DOOR_OPEN = new DoorOpenState();

    /**
     * Returns a cached singleton instance of the requested state.
     */
    public static ElevatorState getState(StateType type) {
        return switch (type) {
            case IDLE -> IDLE;
            case MOVING_UP -> MOVING_UP;
            case MOVING_DOWN -> MOVING_DOWN;
            case DOOR_OPEN -> DOOR_OPEN;
        };
    }

    // Convenience shortcuts
    public static ElevatorState idle() { return IDLE; }
    public static ElevatorState movingUp() { return MOVING_UP; }
    public static ElevatorState movingDown() { return MOVING_DOWN; }
    public static ElevatorState doorOpen() { return DOOR_OPEN; }
}
