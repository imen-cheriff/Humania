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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utilisateur.services.UtilisateurService;
import utils.ImageUtil;
import utils.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GestionUtilisateurController implements Initializable {

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
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u != null && currentUserLabel != null && currentEmailLabel != null) {
            currentUserLabel.setText(u.getUsername() != null ? u.getUsername() : "—");
            currentEmailLabel.setText(u.getEmail() != null ? u.getEmail() : "—");
        }
    }

    private void loadUsers() {
        try {
            users = service.recupererActifs();
        } catch (SQLException e) {
            showError("Impossible de charger les utilisateurs: " + e.getMessage());
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
            String nom = u.getNom() != null ? u.getNom().toLowerCase() : "";
            String prenom = u.getPrenom() != null ? u.getPrenom().toLowerCase() : "";
            String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
            String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
            String numtel = u.getNumtel() != null ? u.getNumtel() : "";

            boolean matchSearch = q.isEmpty()
                    || nom.contains(q) || prenom.contains(q)
                    || email.contains(q) || username.contains(q)
                    || numtel.contains(q);

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
        int to = Math.min(from + PAGE_SIZE, filtered.size());

        countLabel.setText(filtered.size() + " utilisateur" + (filtered.size() > 1 ? "s" : "")
                + " trouvé" + (filtered.size() > 1 ? "s" : ""));
        pageLabel.setText("Page " + currentPage + " sur " + totalPages);
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);

        cardsGrid.getChildren().clear();
        for (Utilisateur u : filtered.subList(from, to)) {
            cardsGrid.getChildren().add(buildCard(u));
        }
    }

    // ---- Card Builder ----
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

        String prenom = u.getPrenom() != null ? u.getPrenom() : "";
        String nom = u.getNom() != null ? u.getNom() : "";
        String statut = u.getStatut() != null ? u.getStatut() : "";
        String displayName = (prenom + " " + nom).trim();
        if (displayName.isEmpty()) displayName = "—";

        // Top: Avatar + Badge
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = ImageUtil.buildCardAvatar(u.getPdp(), prenom, nom);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean actif = "actif".equalsIgnoreCase(statut) || "Actif".equalsIgnoreCase(statut);
        Label badge = new Label(statut.isEmpty() ? "—" : statut);
        badge.setPadding(new Insets(3, 10, 3, 10));
        badge.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 20;" +
                        "-fx-text-fill: %s; -fx-font-size: 10; -fx-font-weight: bold;",
                actif ? "#dcfce7" : "#f3f4f6",
                actif ? "#16a34a" : "#6b7280"));

        top.getChildren().addAll(avatar, spacer, badge);

        // Name + Role + Email
        Label name = new Label(displayName);
        name.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 14; -fx-font-weight: bold;");
        name.setWrapText(true);

        Label role = new Label(u.getRole() != null ? u.getRole().name() : "—");
        role.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11; -fx-font-weight: bold;");

        Label phone = new Label("📞 " + (u.getNumtel() != null && !u.getNumtel().isEmpty() ? u.getNumtel() : "—"));
        phone.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");

        // ── Action buttons — colored & labeled ──
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.setStyle("-fx-border-color: #f3f4f6; -fx-border-width: 1 0 0 0;");
        actions.setVisible(false);

        // Edit — blue pen icon only
        Button editBtn = new Button("✏️");
        editBtn.setStyle(
                "-fx-background-color: #dbeafe;" +
                        "-fx-border-color: #93c5fd; -fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-font-size: 14; -fx-padding: 5 9; -fx-cursor: hand;"
        );
        editBtn.setTooltip(new Tooltip("Modifier"));
        editBtn.setOnAction(e -> openFormDialog(u));

        // Archive — orange archive icon only
        Button archiveBtn = new Button("🗃️");
        archiveBtn.setStyle(
                "-fx-background-color: #ffedd5;" +
                        "-fx-border-color: #fdba74; -fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-font-size: 14; -fx-padding: 5 9; -fx-cursor: hand;"
        );
        archiveBtn.setTooltip(new Tooltip("Archiver"));
        archiveBtn.setOnAction(e -> confirmArchive(u));

        // Delete — red trash icon only
        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle(
                "-fx-background-color: #fee2e2;" +
                        "-fx-border-color: #fca5a5; -fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-font-size: 14; -fx-padding: 5 9; -fx-cursor: hand;"
        );
        deleteBtn.setTooltip(new Tooltip("Supprimer"));
        deleteBtn.setOnAction(e -> confirmDelete(u));

        actions.getChildren().addAll(editBtn, archiveBtn, deleteBtn);

        // Hover effects
        card.setOnMouseEntered(e -> {
            actions.setVisible(true);
            card.setStyle(card.getStyle().replace("-fx-border-color: #e5e7eb;", "-fx-border-color: #c4b5fd;"));
        });
        card.setOnMouseExited(e -> {
            actions.setVisible(false);
            card.setStyle(card.getStyle().replace("-fx-border-color: #c4b5fd;", "-fx-border-color: #e5e7eb;"));
        });
        card.setOnMouseClicked(e -> showDetailDialog(u));

        card.getChildren().addAll(top, name, role, phone, actions);
        return card;
    }

    // ---- Handlers ----
    @FXML
    private void handleOpenProfil(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/user/Profil.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Mon profil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/user/CandidatsAcceptes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Accepted Candidates");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrevPage() { if (currentPage > 1) { currentPage--; refreshGrid(); } }

    @FXML
    private void handleNextPage() { currentPage++; refreshGrid(); }

    // ---- Dialogs ----
    private void showDetailDialog(Utilisateur u) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'utilisateur");
        alert.setHeaderText(u.getPrenom() + " " + u.getNom()
                + " — " + (u.getRole() != null ? u.getRole().name() : "—"));
        alert.setContentText(String.format("""
            Username:     %s
            Email:        %s
            Téléphone:    %s
            Statut:       %s
            Rôle:         %s
            Poste actuel: %s
            Matricule:    %s
            Département:  %s
            Embauche:     %s
            Créé le:      %s
        """,
                u.getUsername(), u.getEmail(),
                u.getNumtel()     != null ? u.getNumtel()               : "—",
                u.getStatut(),
                u.getRole()       != null ? u.getRole().name()          : "—",
                u.getPosteActuel()!= null ? u.getPosteActuel()          : "—",
                u.getMatricule()  != null ? u.getMatricule()            : "—",
                u.getDepartement()!= null ? u.getDepartement()          : "—",
                u.getDateEmbauche()!=null ? u.getDateEmbauche().toString(): "—",
                u.getDateCreation()!=null ? u.getDateCreation().toString(): "—"));
        alert.showAndWait();
    }

    private static class ManagerItem {
        final int id; final String display;
        ManagerItem(int id, String display) { this.id = id; this.display = display; }
        @Override public String toString() { return display; }
    }

    private void openFormDialog(Utilisateur existing) {
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter utilisateur" : "Modifier utilisateur");
        dialog.getDialogPane().setPrefWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // ── Core fields ───────────────────────────────────────────────────────
        TextField nomF      = new TextField(existing != null && existing.getNom()      != null ? existing.getNom()      : "");
        TextField prenomF   = new TextField(existing != null && existing.getPrenom()   != null ? existing.getPrenom()   : "");
        TextField usernameF = new TextField(existing != null && existing.getUsername() != null ? existing.getUsername() : "");
        TextField emailF    = new TextField(existing != null && existing.getEmail()    != null ? existing.getEmail()    : "");
        TextField numtelF   = new TextField(existing != null && existing.getNumtel()   != null ? existing.getNumtel()   : "");

        // Photo
        TextField pdpF = new TextField(existing != null && existing.getPdp() != null ? existing.getPdp() : "");
        pdpF.setPrefWidth(220);
        StackPane photoPreview = ImageUtil.buildCardAvatar(
                existing != null ? existing.getPdp()    : null,
                existing != null ? existing.getPrenom() : "",
                existing != null ? existing.getNom()    : "");
        photoPreview.setPrefWidth(56); photoPreview.setPrefHeight(56);
        Button parcourirBtn = new Button("📁 Parcourir...");
        parcourirBtn.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;" +
                " -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
        parcourirBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.gif"));
            File chosen = fc.showOpenDialog(parcourirBtn.getScene().getWindow());
            if (chosen != null) {
                String uri = chosen.toURI().toString();
                pdpF.setText(uri);
                photoPreview.getChildren().clear();
                photoPreview.getChildren().addAll(ImageUtil.buildCardAvatar(
                        uri, prenomF.getText().trim(), nomF.getText().trim()).getChildren());
            }
        });
        HBox pdpBox = new HBox(8, photoPreview, parcourirBtn);
        pdpBox.setAlignment(Pos.CENTER_LEFT);

        PasswordField pwF = new PasswordField();
        pwF.setPromptText(existing != null ? "Laisser vide = inchangé" : "");

        ComboBox<String> roleBox = new ComboBox<>();
        for (Role r : Role.values()) if (r != Role.CANDIDAT) roleBox.getItems().add(r.name());
        Role defRole = existing != null && existing.getRole() != Role.CANDIDAT ? existing.getRole() : Role.EMPLOYE;
        roleBox.setValue(defRole.name());

        CheckBox statutCb = new CheckBox("Actif");
        statutCb.setSelected(existing == null
                || "actif".equalsIgnoreCase(existing.getStatut())
                || "Actif".equalsIgnoreCase(existing.getStatut()));

        // ── Employee fields ───────────────────────────────────────────────────
        TextField posteActuelF  = new TextField(existing != null && existing.getPosteActuel()  != null ? existing.getPosteActuel()  : "");
        TextField matriculeF    = new TextField(existing != null && existing.getMatricule()    != null ? existing.getMatricule()    : "");
        TextField departementF  = new TextField(existing != null && existing.getDepartement()  != null ? existing.getDepartement()  : "");
        TextField dateEmbaucheF = new TextField(
                existing != null && existing.getDateEmbauche() != null ? existing.getDateEmbauche().toString() : "");
        dateEmbaucheF.setPromptText("AAAA-MM-JJ (optionnel)");

        // Manager dropdown
        ComboBox<ManagerItem> managerBox = new ComboBox<>();
        managerBox.setPromptText("— Aucun manager —");
        managerBox.setPrefWidth(240);
        try {
            List<Utilisateur> managers = service.getManagers();
            for (Utilisateur m : managers) {
                ManagerItem item = new ManagerItem(m.getId(),
                        m.getPrenom() + " " + m.getNom() + "  [" + m.getUsername() + "]");
                managerBox.getItems().add(item);
                if (existing != null && existing.getManagerId() != null
                        && existing.getManagerId() == m.getId()) {
                    managerBox.setValue(item);
                }
            }
        } catch (SQLException e) { showError("Impossible de charger les managers: " + e.getMessage()); }

        // ── Layout ────────────────────────────────────────────────────────────
        Label sep = new Label("── Informations professionnelles ──");
        sep.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11; -fx-font-weight: bold;");

        int r = 0;
        grid.addRow(r++, new Label("Nom *"),          nomF);
        grid.addRow(r++, new Label("Prénom *"),        prenomF);
        grid.addRow(r++, new Label("Username"),        usernameF);
        grid.addRow(r++, new Label("Email *"),         emailF);
        grid.addRow(r++, new Label("Téléphone"),       numtelF);
        grid.addRow(r++, new Label("Photo"),           pdpBox);
        grid.addRow(r++, new Label("Mot de passe"),    pwF);
        grid.addRow(r++, new Label("Rôle"),            roleBox);
        grid.addRow(r++, new Label("Statut"),          statutCb);
        grid.add(sep, 0, r++, 2, 1);
        grid.addRow(r++, new Label("Poste actuel"),    posteActuelF);
        grid.addRow(r++, new Label("Matricule"),       matriculeF);
        grid.addRow(r++, new Label("Département"),     departementF);
        grid.addRow(r++, new Label("Date embauche"),   dateEmbaucheF);
        grid.addRow(r++, new Label("Manager"),         managerBox);

        dialog.getDialogPane().setContent(new ScrollPane(grid));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Utilisateur u = existing != null ? existing : new Utilisateur();
            u.setNom(nomF.getText() != null ? nomF.getText().trim() : "");
            u.setPrenom(prenomF.getText() != null ? prenomF.getText().trim() : "");
            u.setUsername(usernameF.getText() != null ? usernameF.getText().trim() : "");
            u.setEmail(emailF.getText() != null ? emailF.getText().trim() : "");
            u.setNumtel(numtelF.getText() != null ? numtelF.getText().trim() : "");
            u.setPdp(pdpF.getText() != null ? pdpF.getText().trim() : "");
            u.setRole(Role.valueOf(roleBox.getValue()));
            u.setStatut(statutCb.isSelected() ? "Actif" : "Inactif");
            if (pwF.getText() != null && !pwF.getText().isEmpty()) u.setMotDePasse(pwF.getText());
            // Employee fields
            String pa = posteActuelF.getText() != null ? posteActuelF.getText().trim() : "";   u.setPosteActuel(pa.isEmpty()  ? null : pa);
            String ma = matriculeF.getText() != null ? matriculeF.getText().trim() : "";       u.setMatricule(ma.isEmpty()    ? null : ma);
            String dp = departementF.getText() != null ? departementF.getText().trim() : "";   u.setDepartement(dp.isEmpty()  ? null : dp);
            String de = dateEmbaucheF.getText() != null ? dateEmbaucheF.getText().trim() : "";
            if (!de.isEmpty()) {
                try { u.setDateEmbauche(LocalDate.parse(de)); }
                catch (Exception ignored) {}
            } else { u.setDateEmbauche(null); }
            u.setManagerId(managerBox.getValue() != null ? managerBox.getValue().id : null);
            return u;
        });

        dialog.showAndWait().ifPresent(u -> {
            try {
                if (existing != null) service.modifierAvecMotDePasse(u, u.getMotDePasse());
                else service.inserer(u);
                loadUsers();
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });
    }

    private void confirmArchive(Utilisateur u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Archiver");
        confirm.setHeaderText("Archiver l'utilisateur");
        confirm.setContentText("Archiver " + u.getPrenom() + " " + u.getNom()
                + " ? Il ne sera plus affiché dans la gestion des utilisateurs mais sera visible dans les archives.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.archiverUtilisateur(u.getId());
                    loadUsers();
                    showSuccess("Utilisateur archivé.");
                } catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    private void confirmDelete(Utilisateur u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("⚠️ Suppression");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + u.getPrenom() + " " + u.getNom() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try { service.supprimer(u.getId()); loadUsers(); }
                catch (SQLException ex) { showError(ex.getMessage()); }
            }
        });
    }

    @FXML
    private void handleOpenArchives(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/user/archiveUtilisateur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Archives utilisateurs");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- Export ----

    @FXML
    private void handleExportPdf(ActionEvent event) {
        List<Utilisateur> data = getFiltered();
        if (data.isEmpty()) { showError("Aucun utilisateur à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer PDF");
        fc.setInitialFileName("utilisateurs.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Gestion des utilisateurs")
                    .setFontSize(18).setBold()
                    .setFontColor(new DeviceRgb(124, 58, 237))
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            doc.add(new Paragraph(data.size() + " utilisateur(s) exporté(s)")
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            float[] colWidths = {2f, 2f, 2.5f, 2f, 1.5f, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();

            DeviceRgb headerBg = new DeviceRgb(124, 58, 237);
            String[] headers = {"Nom", "Prénom", "Email", "Username", "Rôle", "Statut"};
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold().setFontSize(10)
                                .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBg).setPadding(6));
            }

            DeviceRgb rowWhite = new DeviceRgb(255, 255, 255);
            DeviceRgb rowAlt = new DeviceRgb(245, 243, 255);
            int i = 0;
            for (Utilisateur u : data) {
                DeviceRgb bg = (i++ % 2 == 0) ? rowWhite : rowAlt;
                String[] vals = {
                        safe(u.getNom()), safe(u.getPrenom()), safe(u.getEmail()),
                        safe(u.getUsername()), u.getRole() != null ? u.getRole().name() : "—", safe(u.getStatut())
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
        if (data.isEmpty()) { showError("Aucun utilisateur à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer Excel");
        fc.setInitialFileName("utilisateurs.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog(cardsGrid.getScene().getWindow());
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
            Sheet sheet = wb.createSheet("Utilisateurs");

            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"Nom", "Prénom", "Email", "Username", "Téléphone", "Rôle", "Statut", "Date création"};
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
                        safe(u.getStatut()),
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

    private String safe(String s) { return s != null ? s : "—"; }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}