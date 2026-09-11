package recrutement.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class CandiExtContr {


    public class candidatureCont {

        @FXML
        private Button ajouterCand;

        @FXML
        private Button analyticsBtn;

        @FXML
        private Button bookingsBtn;

        @FXML
        private ComboBox<?> capacityFilter;

        @FXML
        private StackPane centerStack;

        @FXML
        private Button dashboardBtn;

        @FXML
        private ComboBox<?> locationFilter;

        @FXML
        private VBox mainContentVBox;

        @FXML
        private Button refreshBtn;

        @FXML
        private Button roomsBtn;

        @FXML
        private GridPane roomsGrid;

        @FXML
        private TextField searchField;

        @FXML
        private Button settingsBtn;

        @FXML
        private ComboBox<?> statusFilter;

        @FXML
        void handleAddReservation(ActionEvent event) {

        }

        @FXML
        void handleClearFilters(ActionEvent event) {

        }

        @FXML
        void handleRefresh(ActionEvent event) {

        }

    }

}
