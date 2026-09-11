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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utilisateur.enums.Role;
import utilisateur.models.Candidature;
import utilisateur.models.Utilisateur;
import utilisateur.services.CandidatureService;
import utilisateur.services.MailService;
import utilisateur.services.UtilisateurService;
import utilisateur.services.UtilisateurService.ConversionResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CandidatsAcceptesController implements Initializable {

    // ── Manager dropdown wrapper ──────────────────────────────────────────────
    private static class ManagerItem {
        final int    id;
        final String displayName;
        ManagerItem(int id, String displayName) { this.id = id; this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }


    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> etapeFilter;
    @FXML private FlowPane cardsGrid;
    @FXML private Label pageLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private final CandidatureService candidatureService = new CandidatureService();
    private final UtilisateurService  utilisateurService = new UtilisateurService();
    private final MailService         mailService        = new MailService();

    private List<Candidature> acceptedList;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ---------------------------------------------------------------
    // Initialisation
    // ---------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeFilter.getItems().add("Tous les types");
        etapeFilter.getItems().add("Toutes les étapes");

        searchField.textProperty().addListener((o, old, val) -> { currentPage = 1; refreshGrid(); });
        typeFilter.valueProperty().addListener((o, old, val)  -> { currentPage = 1; refreshGrid(); });
        etapeFilter.valueProperty().addListener((o, old, val) -> { currentPage = 1; refreshGrid(); });

        loadTable();
    }

    // ---------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------

    private void loadTable() {
        try {
            acceptedList = candidatureService.getAcceptedCandidatures();

            // Preserve current filter selections
            String typeSel  = typeFilter.getValue();
            String etapeSel = etapeFilter.getValue();

            typeFilter.getItems().clear();
            typeFilter.getItems().add("Tous les types");
            acceptedList.stream()
                    .map(Candidature::getTypeCandidat)
                    .filter(t -> t != null && !t.isEmpty())
                    .distinct().sorted()
                    .forEach(t -> typeFilter.getItems().add(t));
            typeFilter.setValue(typeSel != null && typeFilter.getItems().contains(typeSel)
                    ? typeSel : "Tous les types");

            etapeFilter.getItems().clear();
            etapeFilter.getItems().add("Toutes les étapes");
            acceptedList.stream()
                    .map(Candidature::getEtapePipeline)
                    .filter(e -> e != null && !e.isEmpty())
                    .distinct().sorted()
                    .forEach(e -> etapeFilter.getItems().add(e));
            etapeFilter.setValue(etapeSel != null && etapeFilter.getItems().contains(etapeSel)
                    ? etapeSel : "Toutes les étapes");

        } catch (SQLException e) {
            showError("Impossible de charger les candidats acceptés: " + e.getMessage());
            acceptedList = List.of();
        }
        refreshGrid();
    }

    // ---------------------------------------------------------------
    // Filtering & pagination
    // ---------------------------------------------------------------

    private List<Candidature> getFiltered() {
        if (acceptedList == null) return List.of();
        String q     = searchField.getText().toLowerCase().trim();
        String type  = typeFilter.getValue();
        String etape = etapeFilter.getValue();
        return acceptedList.stream().filter(c -> {
            boolean matchSearch = q.isEmpty()
                    || (c.getNom()          != null && c.getNom().toLowerCase().contains(q))
                    || (c.getPrenom()       != null && c.getPrenom().toLowerCase().contains(q))
                    || (c.getTypeCandidat() != null && c.getTypeCandidat().toLowerCase().contains(q));
            boolean matchType  = type  == null || "Tous les types".equals(type)    || type.equals(c.getTypeCandidat());
            boolean matchEtape = etape == null || "Toutes les étapes".equals(etape) || etape.equals(c.getEtapePipeline());
            return matchSearch && matchType && matchEtape;
        }).collect(Collectors.toList());
    }

    private void refreshGrid() {
        List<Candidature> filtered = getFiltered();
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        currentPage    = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        countLabel.setText(filtered.size() + " candidat(s) trouvé(s)");
        pageLabel.setText("Page " + currentPage + " sur " + totalPages);
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);

        cardsGrid.getChildren().clear();
        filtered.subList(from, to).forEach(c -> cardsGrid.getChildren().add(buildCard(c)));
    }

    // ---------------------------------------------------------------
    // Card builder
    // ---------------------------------------------------------------

    private VBox buildCard(Candidature c) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefWidth(260);
        card.setMinHeight(200);
        card.setMaxWidth(260);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2); -fx-cursor: hand;");

        String nom        = c.getNom()    != null ? c.getNom()    : "";
        String prenom     = c.getPrenom() != null ? c.getPrenom() : "";
        String displayName = (prenom + " " + nom).trim();
        if (displayName.isEmpty()) displayName = "Sans nom";

        Label name = new Label(displayName);
        name.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 15; -fx-font-weight: bold;");
        name.setWrapText(true);

        Label typeLbl = new Label("Type: " +
                (c.getTypeCandidat() != null && !c.getTypeCandidat().isEmpty() ? c.getTypeCandidat() : "—"));
        typeLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        Label etapeLbl = new Label("Étape: " +
                (c.getEtapePipeline() != null && !c.getEtapePipeline().isEmpty() ? c.getEtapePipeline() : "—"));
        etapeLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        Double score = c.getScoringIa();
        Label scoreLbl = new Label("Scoring IA: " +
                (score != null ? String.format("%.0f%%", score) : "—"));
        scoreLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        String dateStr = c.getDateDepot() != null ? c.getDateDepot().format(DATE_FORMAT) : "—";
        Label dateLbl = new Label("Dépôt: " + dateStr);
        dateLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");

        // ---- Convert button: "Déjà converti" only if DB confirms it ----
        boolean converted = c.isAlreadyConverted();
        Button convertBtn = new Button(converted ? "✓ Déjà converti" : "Convertir en utilisateur");
        convertBtn.setDisable(converted);
        convertBtn.setMaxWidth(Double.MAX_VALUE);
        if (converted) {
            convertBtn.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; "
                            + "-fx-background-radius: 6; -fx-cursor: default;");
        } else {
            convertBtn.setStyle(
                    "-fx-background-color: #7c3aed; -fx-text-fill: white; "
                            + "-fx-background-radius: 6; -fx-cursor: hand;");
            convertBtn.setOnAction(e -> openConvertToUserDialog(c));
        }

        // Clicking the card itself (but not if already converted)
        card.setOnMouseClicked(ev -> {
            if (!c.isAlreadyConverted()) openConvertToUserDialog(c);
        });

        card.getChildren().addAll(name, typeLbl, etapeLbl, scoreLbl, dateLbl, convertBtn);
        return card;
    }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; refreshGrid(); } }
    @FXML private void handleNextPage() { currentPage++; refreshGrid(); }

    // ---------------------------------------------------------------
    // Conversion dialog
    // ---------------------------------------------------------------

    private void openConvertToUserDialog(Candidature c) {

        // ---- Guard: double-conversion prevention (DB-level check) ----
        try {
            if (candidatureService.isDejaConverti(c.getId())) {
                showError("Ce candidat a déjà été converti en utilisateur.");
                loadTable();   // refresh to show the updated state
                return;
            }
        } catch (SQLException e) {
            showError("Erreur lors de la vérification: " + e.getMessage());
            return;
        }

        String nomVal    = c.getNom()    != null ? c.getNom()    : "";
        String prenomVal = c.getPrenom() != null ? c.getPrenom() : "";
        String suggestedUsername = (prenomVal + nomVal).toLowerCase().replaceAll("[^a-z0-9]", "");
        if (suggestedUsername.isEmpty()) suggestedUsername = "user";

        // ---- Build dialog ----
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle("Convertir en utilisateur");
        dialog.setHeaderText("Créer un compte utilisateur pour " + prenomVal + " " + nomVal);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Pre-filled, read-only fields from Candidature
        TextField nomF    = new TextField(nomVal);    nomF.setDisable(true);
        TextField prenomF = new TextField(prenomVal); prenomF.setDisable(true);

        String emailCandidat = c.getEmail() != null ? c.getEmail().trim() : "";
        TextField emailPersoF = new TextField(emailCandidat);
        emailPersoF.setPromptText("Email personnel pour envoyer les identifiants *");
        // Allow editing only when the candidature has no email stored
        emailPersoF.setEditable(emailCandidat.isEmpty());

        // Editable fields
        TextField usernameF = new TextField(suggestedUsername);
        usernameF.setPromptText("Identifiant de connexion (laisser vide = auto)");

        ComboBox<String> roleBox = new ComboBox<>();
        for (Role r : Role.values()) {
            if (r != Role.CANDIDAT && r != Role.ADMIN)
                roleBox.getItems().add(r.name());
        }
        roleBox.setValue(Role.EMPLOYE.name());

        ComboBox<String> statutBox = new ComboBox<>();
        statutBox.getItems().addAll("Actif", "Inactif");
        statutBox.setValue("Actif");

        TextField matriculeF   = new TextField(); matriculeF.setPromptText("Matricule");
        TextField posteActuelF = new TextField(); posteActuelF.setPromptText("Poste actuel");
        TextField departementF = new TextField(); departementF.setPromptText("Département");

        // Manager dropdown (replaces raw ID field)
        ComboBox<ManagerItem> managerBox = new ComboBox<>();
        managerBox.setPromptText("— Aucun manager (optionnel) —");
        managerBox.setPrefWidth(240);
        try {
            List<Utilisateur> managers = utilisateurService.getManagers();
            if (managers.isEmpty()) {
                managerBox.setPromptText("— Aucun manager disponible —");
                managerBox.setDisable(true);
            } else {
                for (Utilisateur m : managers)
                    managerBox.getItems().add(new ManagerItem(m.getId(),
                            m.getPrenom() + " " + m.getNom() + "  [" + m.getUsername() + "]"));
            }
        } catch (SQLException e) { showError("Impossible de charger les managers: " + e.getMessage()); }

        Label passwordNote = new Label("🔐 Le mot de passe sera généré et envoyé à l'email ci-dessus (= email de connexion).");
        passwordNote.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11; -fx-wrap-text: true;");

        int row = 0;
        grid.addRow(row++, new Label("Nom *"),              nomF);
        grid.addRow(row++, new Label("Prénom *"),           prenomF);
        grid.addRow(row++, new Label("Email (login) *"),    emailPersoF);
        grid.addRow(row++, new Label("Username"),           usernameF);
        grid.addRow(row++, new Label("Rôle *"),             roleBox);
        grid.addRow(row++, new Label("Statut *"),           statutBox);
        grid.addRow(row++, new Label("Matricule"),          matriculeF);
        grid.addRow(row++, new Label("Poste actuel"),       posteActuelF);
        grid.addRow(row++, new Label("Département"),        departementF);
        grid.addRow(row++, new Label("Manager"),            managerBox);
        grid.add(passwordNote, 0, row++, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // ---- Validation ----
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, ev -> {
            String emailVal = emailPersoF.getText().trim();
            if (emailVal.isEmpty()) {
                showError("L'email est requis — il servira d'identifiant de connexion.");
                ev.consume(); return;
            }
            if (!emailVal.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                showError("L'adresse email saisie n'est pas valide.");
                ev.consume(); return;
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Utilisateur u = new Utilisateur();
            u.setNom(nomVal);
            u.setPrenom(prenomVal);
            u.setRole(Role.valueOf(roleBox.getValue()));
            u.setStatut(statutBox.getValue());
            String usernameInput = usernameF.getText().trim();
            u.setUsername(usernameInput.isEmpty() ? null : usernameInput);
            return u;
        });

        // ---- Process result ----
        dialog.showAndWait().ifPresent(u -> {
            // candidatEmail = the login email AND the address where credentials are sent
            String candidatEmail = emailPersoF.getText().trim();
            try {
                Integer managerId  = managerBox.getValue() != null ? managerBox.getValue().id : null;
                String  matricule  = matriculeF.getText().trim();   if (matricule.isEmpty())  matricule  = null;
                String  posteActuel= posteActuelF.getText().trim(); if (posteActuel.isEmpty()) posteActuel= null;
                String  departement= departementF.getText().trim(); if (departement.isEmpty()) departement= null;

                // All fields written to utilisateur only — no employe/formateur tables
                ConversionResult result = utilisateurService.convertirCandidatEnUtilisateur(
                        u, c.getId(), candidatEmail, managerId, null, matricule, posteActuel, departement);

                // Send credentials to the same email (it IS the login email)
                boolean emailEnvoye = false;
                String  emailErreur = null;
                try {
                    mailService.envoyerCredentials(result.loginEmail(), result.loginEmail(), result.rawPassword());
                    emailEnvoye = true;
                } catch (Exception mailEx) {
                    emailErreur = mailEx.getMessage();
                }

                String managerInfo = managerBox.getValue() != null
                        ? "\nManager assigné : " + managerBox.getValue().displayName : "";

                if (emailEnvoye) {
                    showSuccess("✅ Utilisateur créé avec succès !\n\n"
                            + "Email de connexion : " + result.loginEmail() + "\n"
                            + "Identifiants envoyés à cette adresse." + managerInfo);
                } else {
                    showSuccess("✅ Utilisateur créé.\n\n"
                            + "Email de connexion : " + result.loginEmail() + "\n\n"
                            + "⚠️ Envoi email échoué.\n"
                            + "Mot de passe temporaire : " + result.rawPassword()
                            + "\n\nErreur : " + emailErreur + managerInfo);
                }

                loadTable();

            } catch (SQLException ex) {
                showError("Erreur lors de la conversion : " + ex.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    @FXML
    private void handleBackToGestion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gestionUtilisateur/GestionUtilisateur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des utilisateurs");
            stage.show();
        } catch (IOException e) {
            showError("Impossible de charger la page : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Export PDF
    // ---------------------------------------------------------------

    @FXML
    private void handleExportPdf(ActionEvent event) {
        List<Candidature> data = getFiltered();
        if (data.isEmpty()) { showError("Aucun candidat à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer PDF");
        fc.setInitialFileName("candidats_acceptes.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Candidats Acceptés")
                    .setFontSize(18).setBold()
                    .setFontColor(new DeviceRgb(16, 185, 129))
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            doc.add(new Paragraph(data.size() + " candidat(s) exporté(s)")
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            float[] colWidths = {2f, 2f, 2.5f, 2f, 1.5f, 2f, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();

            DeviceRgb headerBg = new DeviceRgb(16, 185, 129);
            String[] headers = {"Nom", "Prénom", "Email", "Type candidat", "Étape", "Score IA", "Statut"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(10)
                                .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBg).setPadding(6));
            }

            DeviceRgb rowWhite = new DeviceRgb(255, 255, 255);
            DeviceRgb rowAlt   = new DeviceRgb(240, 253, 244);
            int i = 0;
            for (Candidature c : data) {
                DeviceRgb bg = (i++ % 2 == 0) ? rowWhite : rowAlt;
                String score = c.getScoringIa() != null
                        ? String.format("%.1f%%", c.getScoringIa()) : "—";
                String[] vals = {
                        safe(c.getNom()), safe(c.getPrenom()), safe(c.getEmail()),
                        safe(c.getTypeCandidat()), safe(c.getEtapePipeline()),
                        score, safe(c.getStatut())
                };
                for (String v : vals) {
                    table.addCell(new Cell().add(new Paragraph(v).setFontSize(9))
                            .setBackgroundColor(bg).setPadding(5));
                }
            }
            doc.add(table);
            showSuccess("✅ PDF exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Erreur export PDF : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Export Excel
    // ---------------------------------------------------------------

    @FXML
    private void handleExportExcel(ActionEvent event) {
        List<Candidature> data = getFiltered();
        if (data.isEmpty()) { showError("Aucun candidat à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer Excel");
        fc.setInitialFileName("candidats_acceptes.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = wb.createSheet("Candidats acceptés");

            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);
            headerStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Nom", "Prénom", "Email", "Type candidat",
                    "Étape pipeline", "Score IA (%)", "Statut", "Date dépôt"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Candidature c : data) {
                Row row = sheet.createRow(rowIdx);
                CellStyle style = (rowIdx % 2 == 0) ? altStyle : wb.createCellStyle();
                String score = c.getScoringIa() != null
                        ? String.format("%.1f", c.getScoringIa()) : "";
                String date  = c.getDateDepot() != null
                        ? c.getDateDepot().format(dtf) : "";
                String[] vals = {
                        safe(c.getNom()), safe(c.getPrenom()), safe(c.getEmail()),
                        safe(c.getTypeCandidat()), safe(c.getEtapePipeline()),
                        score, safe(c.getStatut()), date
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
            showSuccess("✅ Excel exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Erreur export Excel : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String safe(String s) { return s != null ? s : "—"; }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}