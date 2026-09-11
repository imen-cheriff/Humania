package planification.models;

/**
 * UI-level model representing a single chair in a coworking space.
 */
public class ChairModel {

    public enum State {
        MINE,       // reserved by current user
        AVAILABLE,  // free to reserve
        TAKEN       // reserved by another user
    }

    private final int seatNumber;
    private State state;
    private Integer reservedByUserId;

    public ChairModel(int seatNumber, State state, Integer reservedByUserId) {
        this.seatNumber = seatNumber;
        this.state = state;
        this.reservedByUserId = reservedByUserId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getReservedByUserId() {
        return reservedByUserId;
    }

    public void setReservedByUserId(Integer reservedByUserId) {
        this.reservedByUserId = reservedByUserId;
    }
}

