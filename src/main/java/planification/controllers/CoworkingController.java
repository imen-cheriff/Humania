package planification.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import planification.models.ChairModel;
import planification.models.Espace;
import planification.models.EspaceModel;
import planification.models.ReservationModel;
import planification.services.CoworkingReservationService;
import planification.services.ServiceEspace;
import utils.UserSession;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Conference-table style coworking reservation controller.
 *
 * Seats (circles) are placed directly around a central rounded rectangle
 * that represents the coworking table, matching the reference design:
 *   – top row    : seats above the table (left → right)
 *   – right col  : seats on the right end
 *   – bottom row : seats below the table (right → left)
 *   – left col   : seats on the left end
 *
 * Colours:
 *   Green  (MINE)      : reserved by current user
 *   Yellow (AVAILABLE) : free to reserve
 *   Grey   (TAKEN)     : reserved by another user (disabled)
 */
public class CoworkingController implements Initializable {

    @FXML private Label      titleLabel;
    @FXML private Label      espaceNameLabel;
    @FXML private Label      statusLabel;
    @FXML private DatePicker reservationDatePicker;
    @FXML private Button     refreshButton;
    @FXML private Button     confirmButton;
    @FXML private Button     cancelAllButton;
    @FXML private Pane       seatsPane;

    // ── colours ──────────────────────────────────────────────────────────────
    private static final Color COLOR_MINE      = Color.web("#4CAF50");
    private static final Color COLOR_AVAILABLE = Color.web("#FFC107");
    private static final Color COLOR_TAKEN     = Color.web("#9E9E9E");

    // ── layout constants ─────────────────────────────────────────────────────
    private static final double SEAT_R  = 18;   // seat circle radius
    private static final double GAP     = 8;    // space between table edge and seat edge
    private static final double ROOM_W  = 420;  // table width
    private static final double ROOM_H  = 150;  // table height
    private static final double ROOM_ARC = 80;  // corner arc

    // ── services ─────────────────────────────────────────────────────────────
    private final ServiceEspace               serviceEspace      = new ServiceEspace();
    private final CoworkingReservationService reservationService = new CoworkingReservationService();

    private EspaceModel            currentEspace;
    private final Map<Integer, ChairModel> chairs = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (reservationDatePicker != null) {
            reservationDatePicker.setValue(LocalDate.now());
            reservationDatePicker.valueProperty().addListener((obs, o, n) -> reloadChairs());
        }
        if (currentEspace == null) {
            List<Espace> all = serviceEspace.getAll();
            if (!all.isEmpty()) setEspace(all.get(0));
        } else {
            reloadChairs();
        }
    }

    /** Called externally when the user selects a coworking space card. */
    public void setEspace(Espace espace) {
        this.currentEspace = new EspaceModel(espace);
        if (espaceNameLabel != null) espaceNameLabel.setText(currentEspace.getNom());
        reloadChairs();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML handlers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleRefresh() { reloadChairs(); }

    @FXML
    private void handleConfirm() {
        reloadChairs();
        showInfo("Confirmation", "Les réservations affichées sont à jour.");
    }

    @FXML
    private void handleCancelAll() {
        if (currentEspace == null || reservationDatePicker == null) return;
        Date day = toDate(reservationDatePicker.getValue());
        try {
            reservationService.cancelAllForUser(currentEspace.getId(), UserSession.getInstance().getUserId(), day);
            reloadChairs();
            showInfo("Succès", "Toutes vos réservations pour cette date ont été annulées.");
        } catch (RuntimeException ex) {
            showError("Erreur", ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void reloadChairs() {
        if (currentEspace == null || seatsPane == null || reservationDatePicker == null) return;

        seatsPane.getChildren().clear();
        chairs.clear();

        // ── fetch reservations ───────────────────────────────────────────────
        Date day = toDate(reservationDatePicker.getValue());
        List<ReservationModel> reservations =
                reservationService.getReservationsForDate(currentEspace.getId(), day);
        Map<Integer, ReservationModel> bySeat = new HashMap<>();
        for (ReservationModel r : reservations) bySeat.put(r.getChairNumber(), r);

        int capacity = Math.max(1, currentEspace.getCapacite());

        // ── pane size ────────────────────────────────────────────────────────
        double margin = SEAT_R * 2 + GAP + 24;
        double paneW  = ROOM_W + margin * 2;
        double paneH  = ROOM_H + margin * 2;
        seatsPane.setPrefWidth(paneW);
        seatsPane.setPrefHeight(paneH);

        double cx = paneW / 2.0;
        double cy = paneH / 2.0;

        // ── central table ────────────────────────────────────────────────────
        Rectangle table = new Rectangle(cx - ROOM_W / 2.0, cy - ROOM_H / 2.0, ROOM_W, ROOM_H);
        table.setArcWidth(ROOM_ARC);
        table.setArcHeight(ROOM_ARC);
        table.setFill(Color.web("#FFF9A0"));
        table.setStroke(Color.web("#E0C800"));
        table.setStrokeWidth(2.5);
        seatsPane.getChildren().add(table);

        // Table label
        Text lbl = new Text(currentEspace.getNom());
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        lbl.setFill(Color.web("#666600"));
        lbl.setTextAlignment(TextAlignment.CENTER);
        lbl.setWrappingWidth(ROOM_W - 40);
        lbl.setX(cx - (ROOM_W - 40) / 2.0);
        lbl.setY(cy + 6);
        seatsPane.getChildren().add(lbl);

        // ── distribute seats by perimeter proportion ─────────────────────────
        // perimeter shares: top & bottom proportional to ROOM_W, left & right to ROOM_H
        double perimeter = 2 * (ROOM_W + ROOM_H);
        int nTop    = (int) Math.round(capacity * ROOM_W / perimeter);
        int nBottom = (int) Math.round(capacity * ROOM_W / perimeter);
        int nLeft   = (int) Math.round(capacity * ROOM_H / perimeter);
        int nRight  = capacity - nTop - nBottom - nLeft;
        if (nRight < 0) { nBottom += nRight; nRight = 0; }

        // Build ordered seat position list: top → right → bottom → left
        List<double[]> positions = new ArrayList<>();
        placeRow(positions, nTop,    cx, cy - ROOM_H / 2.0 - GAP - SEAT_R, ROOM_W, true);
        placeCol(positions, nRight,  cx + ROOM_W / 2.0 + GAP + SEAT_R, cy, ROOM_H, true);
        placeRow(positions, nBottom, cx, cy + ROOM_H / 2.0 + GAP + SEAT_R, ROOM_W, false);
        placeCol(positions, nLeft,   cx - ROOM_W / 2.0 - GAP - SEAT_R, cy, ROOM_H, false);

        // ── draw seats ───────────────────────────────────────────────────────
        for (int i = 0; i < positions.size() && i < capacity; i++) {
            int seatNum = i + 1;
            double[] p  = positions.get(i);

            ReservationModel existing = bySeat.get(seatNum);
            ChairModel.State state;
            Integer reservedBy = null;
            if (existing == null) {
                state = ChairModel.State.AVAILABLE;
            } else if (existing.getUserId() == UserSession.getInstance().getUserId()) {
                state = ChairModel.State.MINE;
                reservedBy = existing.getUserId();
            } else {
                state = ChairModel.State.TAKEN;
                reservedBy = existing.getUserId();
            }

            ChairModel chair = new ChairModel(seatNum, state, reservedBy);
            chairs.put(seatNum, chair);
            drawSeat(chair, p[0], p[1]);
        }

        if (statusLabel != null) {
            statusLabel.setText("Cliquez sur une chaise jaune pour réserver, verte pour annuler.");
        }
    }

    /**
     * Distributes {@code count} seats in a horizontal row centred at (rowCX, y),
     * spanning the given width. Pass {@code leftToRight=false} for reverse order.
     */
    private void placeRow(List<double[]> out, int count, double rowCX, double y,
                          double span, boolean leftToRight) {
        if (count <= 0) return;
        double step = span / (count + 1.0);
        for (int i = 1; i <= count; i++) {
            int idx = leftToRight ? i : (count + 1 - i);
            out.add(new double[]{ rowCX - span / 2.0 + idx * step, y });
        }
    }

    /**
     * Distributes {@code count} seats in a vertical column centred at (x, colCY),
     * spanning the given height. Pass {@code topToBottom=false} for reverse order.
     */
    private void placeCol(List<double[]> out, int count, double x, double colCY,
                          double span, boolean topToBottom) {
        if (count <= 0) return;
        double step = span / (count + 1.0);
        for (int i = 1; i <= count; i++) {
            int idx = topToBottom ? i : (count + 1 - i);
            out.add(new double[]{ x, colCY - span / 2.0 + idx * step });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seat rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void drawSeat(ChairModel chair, double x, double y) {
        Circle circle = new Circle(x, y, SEAT_R);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2.5);
        applyColor(circle, chair);

        Tooltip.install(circle, new Tooltip("Chaise " + chair.getSeatNumber()));

        if (chair.getState() == ChairModel.State.TAKEN) {
            circle.setCursor(Cursor.DEFAULT);
            circle.setOpacity(0.60);
        } else {
            circle.setCursor(Cursor.HAND);
            circle.setOnMouseClicked(e -> handleSeatClick(chair, circle));
        }
        seatsPane.getChildren().add(circle);

        // Number label on top of circle
        Text num = new Text(String.valueOf(chair.getSeatNumber()));
        num.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        num.setFill(Color.WHITE);
        num.setMouseTransparent(true);
        num.setX(x - num.getLayoutBounds().getWidth() / 2.0);
        num.setY(y + num.getLayoutBounds().getHeight() / 4.0);
        seatsPane.getChildren().add(num);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interaction
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSeatClick(ChairModel chair, Circle circle) {
        if (currentEspace == null || reservationDatePicker == null) return;
        Date day = toDate(reservationDatePicker.getValue());
        try {
            switch (chair.getState()) {
                case AVAILABLE:
                    reservationService.reserveChair(
                            currentEspace.getId(), chair.getSeatNumber(), UserSession.getInstance().getUserId(), day);
                    chair.setState(ChairModel.State.MINE);
                    chair.setReservedByUserId(UserSession.getInstance().getUserId());
                    break;
                case MINE:
                    reservationService.cancelChair(
                            currentEspace.getId(), chair.getSeatNumber(), UserSession.getInstance().getUserId(), day);
                    chair.setState(ChairModel.State.AVAILABLE);
                    chair.setReservedByUserId(null);
                    break;
                case TAKEN:
                    showInfo("Information", "Cette chaise est déjà réservée par un autre utilisateur.");
                    return;
            }
            applyColor(circle, chair);
        } catch (RuntimeException ex) {
            showError("Erreur", ex.getMessage());
            reloadChairs();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void applyColor(Circle c, ChairModel chair) {
        switch (chair.getState()) {
            case MINE:      c.setFill(COLOR_MINE);      break;
            case AVAILABLE: c.setFill(COLOR_AVAILABLE); break;
            case TAKEN:     c.setFill(COLOR_TAKEN);     break;
        }
    }

    private Date toDate(LocalDate ld) {
        if (ld == null) ld = LocalDate.now();
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}