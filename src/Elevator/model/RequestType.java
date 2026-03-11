package Elevator.model;

/**
 * Distinguishes between requests originating from the floor panel (EXTERNAL)
 * versus inside an elevator cabin (INTERNAL).
 */
public enum RequestType {
    EXTERNAL,   // floor hall-call button press (source floor + direction)
    INTERNAL    // elevator cabin panel press (destination floor from inside a specific elevator)
}
