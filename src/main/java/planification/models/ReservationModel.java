package planification.models;

import java.util.Date;

/**
 * Seat-level reservation for coworking spaces.
 *
 * Backed by a dedicated table, e.g.:
 *   coworking_reservations(
 *      id INT PK AUTO_INCREMENT,
 *      espace_id INT NOT NULL,
 *      chair_number INT NOT NULL,
 *      user_id INT NOT NULL,
 *      reservation_date DATE NOT NULL,
 *      created_at DATETIME NOT NULL,
 *      UNIQUE (espace_id, chair_number, reservation_date)
 *   )
 */
public class ReservationModel {

    private int id;
    private int espaceId;
    private int chairNumber;
    private int userId;
    private Date reservationDate;
    private Date createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEspaceId() {
        return espaceId;
    }

    public void setEspaceId(int espaceId) {
        this.espaceId = espaceId;
    }

    public int getChairNumber() {
        return chairNumber;
    }

    public void setChairNumber(int chairNumber) {
        this.chairNumber = chairNumber;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Date reservationDate) {
        this.reservationDate = reservationDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

