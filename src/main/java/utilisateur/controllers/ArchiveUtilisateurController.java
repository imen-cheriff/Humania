package utilisateur.controllers;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import test.MainFX;
import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utils.UserSession;
import utilisateur.services.UtilisateurService;
import utils.ImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ArchiveUtilisateurController implements Initializable {

    @FXML private Label countLabel, pageLabel;
    @FXML private Label currentUserLabel;
    @FXML private Label currentEmailLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private FlowPane cardsGrid;
    @FXML private Button prevBtn, nextBtn;

    private final UtilisateurService service = new UtilisateurService();
    private List<Utilisateur> users;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleFilter.getItems().add("Tous les rôles");
        for (Role r : Role.values()) {
            if (r != Role.CANDIDAT) roleFilter.getItems().add(r.name());
        }
        roleFilter.setValue("Tous les rôles");

        searchField.textProperty().addListener((o, old, val) -> { currentPage = 1; refreshGrid(); });
        roleFilter.valueProperty().addListener((o, old, val) -> { currentPage = 1; refreshGrid(); });

        bindCurrentUser();
        loadUsers();
    }

    private void bindCurrentUser() {
        UserSession session = UserSession.getInstance();
        String displayName = session.getUser();
        String email       = session.getEmail();
        if (currentUserLabel != null)
            currentUserLabel.setText(displayName != null && !displayName.isBlank() ? displayName : "—");
        if (currentEmailLabel != null)
            currentEmailLabel.setText(email != null && !email.isBlank() ? email : "—");
    }

    private void loadUsers() {
        try {
            users = service.recupererArchives();
        } catch (SQLException e) {
            showError("Impossible de charger les archives: " + e.getMessage());
            users = List.of();
        }
        refreshGrid();
    }

    private List<Utilisateur> getFiltered() {
        if (users == null) return List.of();
        String rawQ = searchField != null && searchField.getText() != null ? searchField.getText() : "";
        String q = rawQ.toLowerCase().trim();
        String role = roleFilter != null ? roleFilter.getValue() : null;

        return users.stream().filter(u -> {
            if (u == null) return false;
            String nom      = u.getNom()      != null ? u.getNom().toLowerCase()      : "";
            String prenom   = u.getPrenom()   != null ? u.getPrenom().toLowerCase()   : "";
            String email    = u.getEmail()    != null ? u.getEmail().toLowerCase()    : "";
            String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
            String numtel   = u.getNumtel()   != null ? u.getNumtel()                 : "";

            boolean matchSearch = q.isEmpty()
                    || nom.contains(q) || prenom.contains(q)
                    || email.contains(q) || username.contains(q) || numtel.contains(q);

            boolean matchRole = role == null || "Tous les rôles".equals(role)
                    || (u.getRole() != null && u.getRole().name().equals(role));
            return matchSearch && matchRole;
        }).collect(Collectors.toList());
    }

    private void refreshGrid() {
        List<Utilisateur> filtered = getFiltered();
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        countLabel.setText(filtered.size() + " utilisateur" + (filtered.size() > 1 ? "s" : "")
                + " archivé" + (filtered.size() > 1 ? "s" : ""));
        pageLabel.setText("Page " + currentPage + " sur " + totalPages);
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);

        cardsGrid.getChildren().clear();
        for (Utilisateur u : filtered.subList(from, to)) {
            cardsGrid.getChildren().add(buildCard(u));
        }
    }

    private VBox buildCard(Utilisateur u) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setPrefWidth(250);
        card.setMinHeight(160);
        card.setMaxWidth(250);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);" +
                        "-fx-cursor: hand;"
        );

        String prenom      = u.getPrenom() != null ? u.getPrenom() : "";
        String nom         = u.getNom()    != null ? u.getNom()    : "";
        String displayName = (prenom + " " + nom).trim();
        if (displayName.isEmpty()) displayName = "—";

        // Top: Avatar + Archive badge
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = ImageUtil.buildArchiveAvatar(u.getPdp(), prenom, nom);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("📦  Archivé");
        badge.setPadding(new Insets(3, 10, 3, 10));
        badge.setStyle(
                "-fx-background-color: #fef3c7; -fx-background-radius: 20;" +
                        "-fx-text-fill: #92400e; -fx-font-size: 10; -fx-font-weight: bold;"
        );

        top.getChildren().addAll(avatar, spacer, badge);

        // Name
        Label name = new Label(displayName);
        name.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 14; -fx-font-weight: bold;");
        name.setWrapText(true);

        // Role — amber accent for archived
        Label role = new Label(u.getRole() != null ? u.getRole().name() : "—");
        role.setStyle("-fx-text-fill: #d97706; -fx-font-size: 11; -fx-font-weight: bold;");

        // Phone
        Label phone = new Label("📞 " + (u.getNumtel() != null && !u.getNumtel().isEmpty() ? u.getNumtel() : "—"));
        phone.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");

        // Actions row
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.setStyle("-fx-border-color: #f3f4f6; -fx-border-width: 1 0 0 0;");
        actions.setVisible(false);

        // Restore — green pill
        Button restoreBtn = new Button("↩️  Restaurer");
        restoreBtn.setStyle(
                "-fx-background-color: #d1fae5;" +
                        "-fx-border-color: #6ee7b7; -fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-text-fill: #065f46; -fx-font-size: 11; -fx-font-weight: bold;" +
                        "-fx-padding: 6 14; -fx-cursor: hand;"
        );
        restoreBtn.setOnAction(e -> confirmRestore(u));

        actions.getChildren().add(restoreBtn);

        // Hover
        card.setOnMouseEntered(e -> {
            actions.setVisible(true);
            card.setStyle(card.getStyle().replace("-fx-border-color: #e5e7eb;", "-fx-border-color: #fcd34d;"));
        });
        card.setOnMouseExited(e -> {
            actions.setVisible(false);
            card.setStyle(card.getStyle().replace("-fx-border-color: #fcd34d;", "-fx-border-color: #e5e7eb;"));
        });
        card.setOnMouseClicked(e -> showDetailDialog(u));

        card.getChildren().addAll(top, name, role, phone, actions);
        return card;
    }

    private void confirmRestore(Utilisateur u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurer");
        confirm.setHeaderText("Restaurer l'utilisateur");
        confirm.setContentText("Restaurer " + u.getPrenom() + " " + u.getNom()
                + " ? Il sera à nouveau visible dans la gestion des utilisateurs (statut Actif).");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.restaurerUtilisateur(u.getId());
                    loadUsers();
                    showSuccess("Utilisateur restauré.");
                } catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    private void showDetailDialog(Utilisateur u) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'utilisateur archivé");
        alert.setHeaderText(u.getPrenom() + " " + u.getNom() + " — " + (u.getRole() != null ? u.getRole().name() : "—"));
        alert.setContentText(String.format("""
            Username:  %s
            Email:     %s
            Téléphone: %s
            Statut:    %s
            Créé le:   %s
        """, u.getUsername(), u.getEmail(),
                u.getNumtel() != null ? u.getNumtel() : "N/A",
                u.getStatut(),
                u.getDateCreation() != null ? u.getDateCreation().toString() : "N/A"));
        alert.showAndWait();
    }

    @FXML
    private void handleOpenProfil(ActionEvent event) {
        if (MainFX.getInstance() != null)
            MainFX.getInstance().navigateTo(MainFX.PROFIL);
    }

    @FXML
    private void handleBackToGestion(ActionEvent event) {
        if (MainFX.getInstance() != null)
            MainFX.getInstance().navigateTo(MainFX.GESTION_UTILISATEUR);
    }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; refreshGrid(); } }
    @FXML private void handleNextPage() { currentPage++; refreshGrid(); }

    // ---- Export ----

    @FXML
    private void handleExportPdf(ActionEvent event) {
        List<Utilisateur> data = getFiltered();
        if (data.isEmpty()) { showError("Aucun utilisateur archivé à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer PDF");
        fc.setInitialFileName("archives_utilisateurs.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Archives des utilisateurs")
                    .setFontSize(18).setBold()
                    .setFontColor(new DeviceRgb(217, 119, 6))
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            doc.add(new Paragraph(data.size() + " utilisateur(s) archivé(s)")
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            float[] colWidths = {2f, 2f, 2.5f, 2f, 1.5f, 2f};
            Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();

            DeviceRgb headerBg = new DeviceRgb(217, 119, 6);
            String[] headers = {"Nom", "Prénom", "Email", "Username", "Rôle", "Date création"};
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold().setFontSize(10)
                                .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBg).setPadding(6));
            }

            DeviceRgb rowWhite = new DeviceRgb(255, 255, 255);
            DeviceRgb rowAlt = new DeviceRgb(255, 251, 235);
            int i = 0;
            for (Utilisateur u : data) {
                DeviceRgb bg = (i++ % 2 == 0) ? rowWhite : rowAlt;
                String[] vals = {
                        safe(u.getNom()), safe(u.getPrenom()), safe(u.getEmail()),
                        safe(u.getUsername()), u.getRole() != null ? u.getRole().name() : "—",
                        u.getDateCreation() != null ? u.getDateCreation().toString() : "—"
                };
                for (String v : vals) {
                    table.addCell(new Cell().add(new Paragraph(v).setFontSize(9))
                            .setBackgroundColor(bg).setPadding(5));
                }
            }
            doc.add(table);
            showSuccess("PDF exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) { showError("Erreur export PDF : " + e.getMessage()); }
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        List<Utilisateur> data = getFiltered();
        if (data.isEmpty()) { showError("Aucun utilisateur archivé à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer Excel");
        fc.setInitialFileName("archives_utilisateurs.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
            Sheet sheet = wb.createSheet("Archives");

            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"Nom", "Prénom", "Email", "Username", "Téléphone", "Rôle", "Date création"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Utilisateur u : data) {
                Row row = sheet.createRow(rowIdx);
                CellStyle style = (rowIdx % 2 == 0) ? altStyle : wb.createCellStyle();
                String[] vals = {
                        safe(u.getNom()), safe(u.getPrenom()), safe(u.getEmail()),
                        safe(u.getUsername()), safe(u.getNumtel()),
                        u.getRole() != null ? u.getRole().name() : "—",
                        u.getDateCreation() != null ? u.getDateCreation().toString() : "—"
                };
                for (int i = 0; i < vals.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    cell.setCellValue(vals[i]);
                    cell.setCellStyle(style);
                }
                rowIdx++;
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(fos);
            showSuccess("Excel exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) { showError("Erreur export Excel : " + e.getMessage()); }
    }

    private String safe(String s) { return s != null ? s : ""; }

    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}