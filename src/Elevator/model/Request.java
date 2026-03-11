package Elevator.model;

/**
 * Represents a request made either from a floor (EXTERNAL) or from inside an elevator (INTERNAL).
 */
public class Request {
    private final int sourceFloor;
    private final int destinationFloor;
    private final Direction direction;
    private final RequestType type;
    private final String elevatorId; // non-null only for INTERNAL requests

    /**
     * Factory: External request — person presses UP/DOWN on a floor, then selects destination.
     */
    public static Request external(int sourceFloor, int destinationFloor) {
        Direction dir = destinationFloor > sourceFloor ? Direction.UP : Direction.DOWN;
        return new Request(sourceFloor, destinationFloor, dir, RequestType.EXTERNAL, null);
    }

    /**
     * Factory: Internal request — person inside elevator presses a floor button.
     */
    public static Request internal(String elevatorId, int currentFloor, int destinationFloor) {
        Direction dir = destinationFloor > currentFloor ? Direction.UP : Direction.DOWN;
        return new Request(currentFloor, destinationFloor, dir, RequestType.INTERNAL, elevatorId);
    }

    private Request(int sourceFloor, int destinationFloor, Direction direction,
                    RequestType type, String elevatorId) {
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        this.direction = direction;
        this.type = type;
        this.elevatorId = elevatorId;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public RequestType getType() {
        return type;
    }

    public String getElevatorId() {
        return elevatorId;
    }

    @Override
    public String toString() {
        String tag = type == RequestType.INTERNAL ? "INT[" + elevatorId + "]" : "EXT";
        return "Request{" + tag + " from=" + sourceFloor + ", to=" + destinationFloor + ", dir=" + direction + "}";
    }
}
