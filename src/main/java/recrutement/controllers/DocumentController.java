package recrutement.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import recrutement.models.Document;
import recrutement.services.ServiceDocument;
import recrutement.services.SignNowService;

import java.awt.Desktop;
import java.net.URI;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur Documents — Upload PDF → DB → affichage + ✍ Signature SignNow.
 */
public class DocumentController implements Initializable {

    @FXML private Button           btnUploader;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private VBox             documentsList;
    @FXML private Label            statTotal;
    @FXML private Label            statSignes;
    @FXML private Label            statEnAttente;
    @FXML private Label            statNonSignes;

    private final SimpleDateFormat DF      = new SimpleDateFormat("dd/MM/yyyy");
    private final ServiceDocument  service = new ServiceDocument();
    private final SignNowService   signNow = new SignNowService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbFilter.getItems().setAll("Tous", "PDF", "Doc", "Image", "Fichier");
        cmbFilter.setValue("Tous");
        loadDocuments();
    }

    private void loadDocuments() {
        documentsList.getChildren().clear();
        try {
            List<Document> docs = service.getAll();
            for (Document d : docs) addDocumentRow(d);
            updateStats(docs.size());
        } catch (Exception ex) {
            System.err.println("Erreur chargement : " + ex.getMessage());
            updateStats(0);
        }
    }

    private void updateStats(int total) {
        if (statTotal    != null) statTotal.setText(String.valueOf(total));
        if (statSignes   != null) statSignes.setText("0");
        if (statEnAttente!= null) statEnAttente.setText("0");
        if (statNonSignes!= null) statNonSignes.setText(String.valueOf(total));
    }

    @FXML
    public void handleUploader(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner des documents PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));

        List<File> files = chooser.showOpenMultipleDialog(btnUploader.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        int count = 0;
        for (File f : files) {
            if (!f.getName().toLowerCase().endsWith(".pdf")) {
                showAlert(Alert.AlertType.WARNING, "Type non autorisé",
                        "Seuls les PDF sont acceptés.\nFichier ignoré : " + f.getName());
                continue;
            }
            try {
                Document saved = service.add(f);  // copie fichier + INSERT DB
                addDocumentRow(saved);
                count++;
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur upload",
                        "Impossible d'enregistrer : " + f.getName() + "\n" + ex.getMessage());
            }
        }
        if (count > 0) {
            try { updateStats(service.getAll().size()); } catch (Exception ignored) {}
            showAlert(Alert.AlertType.INFORMATION, "Upload réussi",
                    count + " document(s) enregistré(s) en base de données.");
        }
    }

    private void addDocumentRow(Document doc) {
        File   file    = new File(doc.getPath());
        String name    = doc.getName() != null ? doc.getName() : file.getName();
        String dateStr = doc.getUploadedAt() != null
                ? DF.format(new Date(doc.getUploadedAt().getTime())) : DF.format(new Date());
        String type    = doc.getType() != null ? doc.getType() : "PDF";

        // Icône
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(42, 42);
        Circle circle = new Circle(21);
        circle.setStyle("-fx-fill: #eff6ff;");
        Label emoji = new Label("📄");
        emoji.setStyle("-fx-font-size: 18px;");
        iconPane.getChildren().addAll(circle, emoji);

        // Nom + meta
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        Label lblMeta = new Label(dateStr + " · Stocké en DB");
        lblMeta.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-text-fill:#94a3b8;");
        VBox metaBox = new VBox(2, lblName, lblMeta);
        HBox.setHgrow(metaBox, Priority.ALWAYS);

        // Badge
        Label badge = new Label(type);
        badge.setStyle("-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1; -fx-font-size:11px;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 14 4 14;");

        // Statut signature
        Label lblSignStatus = new Label("");
        lblSignStatus.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; -fx-min-width:80;");

        // Bouton Ouvrir
        Button btnOpen = new Button("⬇ Ouvrir");
        btnOpen.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#3b82f6;"
                + "-fx-font-size:12px; -fx-font-weight:700; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-padding:6 14 6 14;");
        btnOpen.setOnAction(e -> {
            try {
                if (file.exists() && Desktop.isDesktopSupported())
                    Desktop.getDesktop().open(file);
                else
                    showAlert(Alert.AlertType.WARNING, "Fichier introuvable",
                            "Le fichier n'est plus accessible :\n" + doc.getPath());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur ouverture", ex.getMessage());
            }
        });

        // Bouton Renommer
        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color:transparent; -fx-text-fill:#f59e0b;"
                + "-fx-font-size:15px; -fx-cursor:hand; -fx-padding:4 6;");
        btnEdit.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(lblName.getText());
            dlg.setTitle("Renommer");
            dlg.setHeaderText("Renommer le document");
            dlg.setContentText("Nouveau nom :");
            dlg.showAndWait().ifPresent(lblName::setText);
        });

        // ── Bouton Signer (SignNow) ───────────────────────────────────────
        Button btnSign = new Button("✍ Signer");
        btnSign.setStyle(
                "-fx-background-color: #16a34a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-cursor: hand;");
        btnSign.setOnAction(e -> handleSign(file, btnSign, lblSignStatus));

        // Bouton Supprimer
        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color:transparent; -fx-text-fill:#ef4444;"
                + "-fx-font-size:15px; -fx-cursor:hand; -fx-padding:4 6;");

        // Assemblage
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(72);
        row.setStyle("-fx-background-color:white; -fx-padding:0 20 0 16;"
                + "-fx-border-color:transparent transparent #f1f5f9 transparent;"
                + "-fx-border-width:1;");
        row.getChildren().addAll(iconPane, metaBox, badge, lblSignStatus,
                btnOpen, btnEdit, btnSign, btnDel);

        btnDel.setOnAction(ev -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer");
            confirm.setHeaderText("Supprimer ce document définitivement ?");
            confirm.setContentText(name + "\n\nCette action supprimera le document de la base de données.");
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    try {
                        service.delete(doc.getId(), doc.getPath()); // ← DELETE en DB + fichier
                        documentsList.getChildren().remove(row);
                        try { updateStats(service.getAll().size()); } catch (Exception ignored) {}
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur suppression",
                                "Impossible de supprimer : " + ex.getMessage());
                    }
                }
            });
        });

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f8faff; -fx-padding:0 20 0 16;"
                + "-fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:1;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:white; -fx-padding:0 20 0 16;"
                + "-fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:1;"));

        documentsList.getChildren().add(row);
    }

    // ── Logique signature SignNow ─────────────────────────────────────────
    private void handleSign(File file, Button btnSign, Label lblSignStatus) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("✍ Signature électronique — SignNow");
        dialog.setHeaderText("Envoyer « " + file.getName() + " » pour signature");

        ButtonType btnSend = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSend, ButtonType.CANCEL);

        TextField fldFirstName = new TextField(); fldFirstName.setPromptText("Prénom");
        TextField fldLastName  = new TextField(); fldLastName.setPromptText("Nom");
        TextField fldEmail     = new TextField(); fldEmail.setPromptText("email@exemple.com");

        Label note = new Label("📧 Le signataire recevra un email SignNow avec son lien de signature.");
        note.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a; -fx-wrap-text:true;");
        note.setMaxWidth(360);

        VBox content = new VBox(10,
                new Label("Prénom du signataire :"), fldFirstName,
                new Label("Nom du signataire :"),    fldLastName,
                new Label("Email du signataire :"),  fldEmail,
                new Separator(), note);
        content.setStyle("-fx-padding:12;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(btn -> btn == btnSend
                ? new String[]{ fldFirstName.getText().trim(),
                fldLastName.getText().trim(),
                fldEmail.getText().trim() }
                : null);

        dialog.showAndWait().ifPresent(result -> {
            String firstName = result[0];
            String lastName  = result[1];
            String email     = result[2];

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champs requis",
                        "Veuillez renseigner le prénom, le nom et l'email.");
                return;
            }
            if (!email.contains("@")) {
                showAlert(Alert.AlertType.WARNING, "Email invalide",
                        "L'adresse email semble incorrecte.");
                return;
            }

            btnSign.setDisable(true);
            btnSign.setText("⏳ Envoi...");
            lblSignStatus.setText("En cours...");

            new Thread(() -> {
                try {
                    SignNowService.SignatureResult res =
                            signNow.sendForSignature(file, firstName + " " + lastName, email);

                    Platform.runLater(() -> {
                        btnSign.setDisable(false);
                        btnSign.setText("✅ Envoyé");
                        btnSign.setStyle(
                                "-fx-background-color: #22c55e;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-font-weight: 700;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-padding: 6 14 6 14;");

                        boolean isDemo = res.documentId != null && res.documentId.startsWith("demo-");
                        if (isDemo) {
                            lblSignStatus.setText("✅ Signé");
                            lblSignStatus.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a; -fx-font-weight:700;");
                        } else {
                            lblSignStatus.setText("🟡 En attente");
                            lblSignStatus.setStyle("-fx-font-size:11px; -fx-text-fill:#f59e0b;");
                        }

                        Alert linkAlert = new Alert(Alert.AlertType.INFORMATION);
                        linkAlert.setTitle("✅ Demande envoyée — SignNow");
                        linkAlert.setHeaderText("Demande de signature envoyée avec succès !");
                        String alertContent = "📧 Email envoyé à : " + email
                                + "\n📋 ID document : " + res.documentId;
                        if (res.signingLink != null && !res.signingLink.isEmpty())
                            alertContent += "\n🔗 Lien de signature :\n" + res.signingLink;
                        linkAlert.setContentText(alertContent);
                        linkAlert.getDialogPane().setPrefWidth(500);

                        if (res.signingLink != null && !res.signingLink.isEmpty()) {
                            ButtonType btnOpenLink = new ButtonType("Ouvrir dans le navigateur");
                            linkAlert.getButtonTypes().add(btnOpenLink);
                            linkAlert.showAndWait().ifPresent(btn -> {
                                if (btn == btnOpenLink) {
                                    try { Desktop.getDesktop().browse(new URI(res.signingLink)); }
                                    catch (Exception ex) { System.err.println("Erreur navigateur : " + ex.getMessage()); }
                                }
                            });
                        } else {
                            linkAlert.showAndWait();
                        }
                    });

                } catch (Exception ex) {
                    System.err.println("Erreur SignNow: " + ex.getMessage());
                    Platform.runLater(() -> {
                        btnSign.setDisable(false);
                        btnSign.setText("✍ Signer");
                        btnSign.setStyle(
                                "-fx-background-color: #16a34a;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-font-weight: 700;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-padding: 6 14 6 14;");
                        lblSignStatus.setText("");
                        showAlert(Alert.AlertType.ERROR, "Signature indisponible",
                                "Le service SignNow n'a pas pu traiter la demande.\n"
                                        + "Votre document est bien enregistré en DB.\n\n"
                                        + "Détail : " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    @FXML
    public void handleSearch() {
        String query = txtSearch.getText().toLowerCase().trim();
        documentsList.getChildren().forEach(node -> {
            if (node instanceof HBox row) {
                VBox meta = (VBox) row.getChildren().get(1);
                String rowName = ((Label) meta.getChildren().get(0)).getText().toLowerCase();
                boolean show = query.isEmpty() || rowName.contains(query);
                row.setVisible(show);
                row.setManaged(show);
            }
        });
    }

    @FXML
    public void handleFilter(ActionEvent event) {
        String filter = cmbFilter.getValue();
        documentsList.getChildren().forEach(node -> {
            if (node instanceof HBox row && row.getChildren().size() >= 3) {
                String badgeText = ((Label) row.getChildren().get(2)).getText();
                boolean show = "Tous".equals(filter) || badgeText.equalsIgnoreCase(filter);
                row.setVisible(show);
                row.setManaged(show);
            }
        });
    }

    @FXML public void handleDownload(ActionEvent event) {}
    @FXML public void handleEdit(ActionEvent event) {}
    @FXML public void handleDelete(ActionEvent event) {}

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}