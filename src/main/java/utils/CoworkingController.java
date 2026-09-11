package utils;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import planification.models.ChairModel;
import planification.models.Espace;
import planification.models.EspaceModel;
import planification.models.ReservationModel;
import planification.services.CoworkingReservationService;
import planification.services.ServiceEspace;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Cinema-style seat reservation controller for coworking spaces.
 *
 * The selected {@link Espace} is mapped to an {@link EspaceModel} and all
 * chairs are generated dynamically based on its capacity. Each seat shows:
 *
 *  - Green  (MINE)      : reserved by current user
 *  - Yellow (AVAILABLE) : free
 *  - Grey   (TAKEN)     : reserved by another user (disabled)
 *
 * Reservations are stored per day and per chair.
 */
public class CoworkingController implements Initializable {

    @FXML
    private Label titleLabel;

    @FXML
    private Label espaceNameLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private DatePicker reservationDatePicker;

    @FXML
    private Button refreshButton;

    @FXML
    private Button confirmButton;

    @FXML
    private Button cancelAllButton;

    @FXML
    private GridPane seatsGrid;

    private final ServiceEspace serviceEspace = new ServiceEspace();
    private final CoworkingReservationService reservationService = new CoworkingReservationService();

    private EspaceModel currentEspace;
    private final Map<Integer, ChairModel> chairs = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (reservationDatePicker != null) {
            reservationDatePicker.setValue(LocalDate.now());
            reservationDatePicker.valueProperty().addListener((obs, oldV, newV) -> reloadChairs());
        }
        // If no espace has been injected, try first available one for admin/demo
        if (currentEspace == null) {
            List<Espace> espaces = serviceEspace.getAll();
            if (!espaces.isEmpty()) {
                setEspace(espaces.get(0));
            }
        } else {
            reloadChairs();
        }
    }

    /**
     * Called externally when user clicks a coworking card from the main page.
     */
    public void setEspace(Espace espace) {
        this.currentEspace = new EspaceModel(espace);
        if (espaceNameLabel != null) {
            espaceNameLabel.setText(currentEspace.getNom());
        }
        reloadChairs();
    }

    @FXML
    private void handleRefresh() {
        reloadChairs();
    }

    @FXML
    private void handleConfirm() {
        // For now, confirmation simply reloads from DB to reflect final state
        reloadChairs();
        showInfo("Confirmation", "Les réservations affichées sont à jour.");
    }

    @FXML
    private void handleCancelAll() {
        if (currentEspace == null || reservationDatePicker == null) {
            return;
        }
        Date day = toDate(reservationDatePicker.getValue());
        try {
            reservationService.cancelAllForUser(currentEspace.getId(), CurrentUser.getId(), day);
            reloadChairs();
            showInfo("Succès", "Toutes vos réservations pour cette date ont été annulées.");
        } catch (RuntimeException ex) {
            showError("Erreur", ex.getMessage());
        }
    }

    private void reloadChairs() {
        if (currentEspace == null || seatsGrid == null || reservationDatePicker == null) {
            return;
        }
        seatsGrid.getChildren().clear();
        chairs.clear();

        Date day = toDate(reservationDatePicker.getValue());
        List<ReservationModel> reservations =
                reservationService.getReservationsForDate(currentEspace.getId(), day);

        Map<Integer, ReservationModel> bySeat = new HashMap<>();
        for (ReservationModel r : reservations) {
            bySeat.put(r.getChairNumber(), r);
        }

        int capacity = Math.max(1, currentEspace.getCapacite());
        int columns = 10; // cinema-style: 10 seats per row
        int row = 0;
        int col = 0;

        for (int seatNumber = 1; seatNumber <= capacity; seatNumber++) {
            ReservationModel existing = bySeat.get(seatNumber);
            ChairModel.State state;
            Integer reservedBy = null;
            if (existing == null) {
                state = ChairModel.State.AVAILABLE;
            } else if (existing.getUserId() == CurrentUser.getId()) {
                state = ChairModel.State.MINE;
                reservedBy = existing.getUserId();
            } else {
                state = ChairModel.State.TAKEN;
                reservedBy = existing.getUserId();
            }

            ChairModel chair = new ChairModel(seatNumber, state, reservedBy);
            chairs.put(seatNumber, chair);

            Button seatBtn = buildSeatButton(chair);
            seatsGrid.add(seatBtn, col, row);
            GridPane.setHalignment(seatBtn, HPos.CENTER);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }

        if (statusLabel != null) {
            statusLabel.setText("Sélectionnez / désélectionnez une chaise puis cliquez sur Confirmer si besoin.");
        }
    }

    private Button buildSeatButton(ChairModel chair) {
        Button btn = new Button(String.valueOf(chair.getSeatNumber()));
        btn.getStyleClass().add("seat-button");
        updateSeatStyle(btn, chair);
        btn.setMinSize(28, 28);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(36, 36);
        btn.setPadding(new Insets(4));

        btn.setOnAction(e -> handleSeatClick(chair, btn));
        return btn;
    }

    private void handleSeatClick(ChairModel chair, Button btn) {
        if (currentEspace == null || reservationDatePicker == null) {
            return;
        }
        Date day = toDate(reservationDatePicker.getValue());

        try {
            switch (chair.getState()) {
                case AVAILABLE:
                    // Try to reserve
                    reservationService.reserveChair(currentEspace.getId(),
                            chair.getSeatNumber(),
                            CurrentUser.getId(),
                            day);
                    chair.setState(ChairModel.State.MINE);
                    chair.setReservedByUserId(CurrentUser.getId());
                    break;
                case MINE:
                    // Cancel own reservation
                    reservationService.cancelChair(currentEspace.getId(),
                            chair.getSeatNumber(),
                            CurrentUser.getId(),
                            day);
                    chair.setState(ChairModel.State.AVAILABLE);
                    chair.setReservedByUserId(null);
                    break;
                case TAKEN:
                    // Do nothing if reserved by another user
                    showInfo("Information", "Cette chaise est déjà réservée par un autre utilisateur.");
                    return;
            }
            updateSeatStyle(btn, chair);
        } catch (RuntimeException ex) {
            showError("Erreur", ex.getMessage());
            // In case of concurrency conflict, reload full state from DB
            reloadChairs();
        }
    }

    private void updateSeatStyle(Button btn, ChairModel chair) {
        btn.getStyleClass().removeAll("seat-available", "seat-mine", "seat-taken");
        switch (chair.getState()) {
            case MINE:
                btn.getStyleClass().add("seat-mine");
                btn.setDisable(false);
                break;
            case AVAILABLE:
                btn.getStyleClass().add("seat-available");
                btn.setDisable(false);
                break;
            case TAKEN:
                btn.getStyleClass().add("seat-taken");
                btn.setDisable(true);
                break;
        }
    }

    private Date toDate(LocalDate ld) {
        if (ld == null) {
            ld = LocalDate.now();
        }
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

