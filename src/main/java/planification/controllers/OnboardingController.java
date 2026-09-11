package planification.controllers;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import planification.services.AIService;
import recrutement.models.CandidatureExterne;
import recrutement.services.ServiceCandidatureExterne;
import utils.EventBus;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Onboarding controller — generates a personalised onboarding checklist.
 * AI enhancement: after generating the standard checklist, an
 * "🤖 Enrichir avec l'IA" button calls Groq to append extra role-specific tasks.
 */
public class OnboardingController implements Initializable {

    // ── FXML nodes ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> employeeCombo;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private DatePicker       arrivalDatePicker;
    @FXML private Button           generateBtn;
    @FXML private HBox             statsRow;
    @FXML private VBox             taskPanel;
    @FXML private VBox             welcomeCard;
    @FXML private VBox             autoActionsCard;

    // ── State ─────────────────────────────────────────────────────────────
    private final List<TaskItem> currentTasks = new ArrayList<>();
    private final AIService                aiService    = new AIService();
    private final ServiceCandidatureExterne serviceCand  = new ServiceCandidatureExterne();

    // Tracks the last employee/dept used for AI so we can pass them to the service
    private String lastEmployee;
    private String lastDept;

    // ── Mock data ─────────────────────────────────────────────────────────
    private final Map<String, String[]> EMPLOYEES = new LinkedHashMap<>() {{
        put("Alice Martin",    new String[]{"Ingénierie", "alice.martin@humania.com",   "Développeuse Senior"});
        put("Karim Bouzidi",   new String[]{"Marketing",  "karim.bouzidi@humania.com",  "Chef de projet"});
        put("Sofia Trabelsi",  new String[]{"RH",         "sofia.trabelsi@humania.com", "Responsable RH"});
        put("Lucas Bernard",   new String[]{"Finance",    "lucas.bernard@humania.com",  "Analyste financier"});
        put("Yasmine Chouari", new String[]{"Ventes",     "yasmine.chouari@humania.com","Commerciale"});
        put("Mohamed Haddad",  new String[]{"Ingénierie", "m.haddad@humania.com",       "DevOps Engineer"});
    }};

    private static final String[] DEPARTMENTS =
            {"Ingénierie", "Marketing", "RH", "Finance", "Ventes", "Support", "Juridique"};

    private static final List<String[]> COMMON_TASKS = List.of(
            new String[]{"📋", "Signature du contrat de travail",         "Administratif", "0"},
            new String[]{"🪪", "Création du badge d'accès",               "Accès",         "0"},
            new String[]{"📧", "Création du compte email professionnel",  "IT",            "0"},
            new String[]{"🖥",  "Configuration du poste de travail",       "IT",            "1"},
            new String[]{"📚", "Remise du livret d'accueil",              "RH",            "1"},
            new String[]{"👥", "Présentation à l'équipe",                 "Intégration",   "1"},
            new String[]{"🏥", "Affiliation mutuelle / prévoyance",       "Administratif", "2"},
            new String[]{"🔐", "Accès aux outils internes (intranet…)",   "IT",            "2"},
            new String[]{"🧭", "Visite des locaux et règles de sécurité", "Intégration",   "3"},
            new String[]{"📝", "Entretien d'intégration J+30",            "RH",            "30"}
    );

    private static final Map<String, List<String[]>> DEPT_TASKS = new HashMap<>();
    static {
        DEPT_TASKS.put("Ingénierie", List.of(
                new String[]{"💻", "Accès GitHub / GitLab",               "IT",        "1"},
                new String[]{"🐳", "Installation environnement dev",      "IT",        "2"},
                new String[]{"📖", "Lecture de la doc technique",         "Formation", "3"},
                new String[]{"🔑", "Accès aux serveurs de staging",       "IT",        "5"},
                new String[]{"🤝", "Pair-programming avec le mentor",     "Formation", "7"}
        ));
        DEPT_TASKS.put("Marketing", List.of(
                new String[]{"🎨", "Accès Canva / Adobe Suite",           "IT",        "1"},
                new String[]{"📊", "Formation Google Analytics",          "Formation", "3"},
                new String[]{"📱", "Accès réseaux sociaux d'entreprise",  "IT",        "2"},
                new String[]{"🗓", "Calendrier éditorial partagé",        "Intégration","3"}
        ));
        DEPT_TASKS.put("RH", List.of(
                new String[]{"🗂", "Accès SIRH",                          "IT",        "1"},
                new String[]{"⚖️", "Formation droit du travail interne",  "Formation", "5"},
                new String[]{"🔒", "Formation RGPD données RH",           "Formation", "7"},
                new String[]{"📂", "Accès dossiers du personnel",         "Accès",     "2"}
        ));
        DEPT_TASKS.put("Finance", List.of(
                new String[]{"💰", "Accès logiciel comptable",            "IT",        "1"},
                new String[]{"📈", "Accès tableaux de bord financiers",   "IT",        "2"},
                new String[]{"🏦", "Formation procédures de paiement",    "Formation", "5"}
        ));
        DEPT_TASKS.put("Ventes", List.of(
                new String[]{"📞", "Accès CRM",                           "IT",        "1"},
                new String[]{"🎯", "Formation produits & services",       "Formation", "3"},
                new String[]{"🤝", "Accompagnement commercial terrain",   "Formation", "7"},
                new String[]{"📊", "Attribution du portefeuille clients", "Intégration","10"}
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        employeeCombo.getItems().addAll(EMPLOYEES.keySet());
        departmentCombo.getItems().addAll(DEPARTMENTS);
        arrivalDatePicker.setValue(LocalDate.now());

        // ── Charger les candidats acceptés depuis la DB ───────────────────
        loadAcceptedCandidates();

        // ── Écouter l'EventBus pour se mettre à jour en temps réel ────────
        EventBus.addCandidatureListener(() ->
                Platform.runLater(this::loadAcceptedCandidates));

        employeeCombo.valueProperty().addListener((obs, o, name) -> {
            if (name != null && EMPLOYEES.containsKey(name)) {
                departmentCombo.setValue(EMPLOYEES.get(name)[0]);
            }
        });

        showEmptyState();
    }

    /**
     * Charge les candidatures avec statut "Acceptée" depuis la DB
     * et les ajoute dans employeeCombo (sans dupliquer les entrées mock).
     */
    private void loadAcceptedCandidates() {
        try {
            List<CandidatureExterne> acceptes = serviceCand.getAll().stream()
                    .filter(c -> "Acceptée".equals(c.getStatut()))
                    .collect(java.util.stream.Collectors.toList());

            // Retirer les entrées DB précédentes (marquées avec le préfixe "📌 ")
            employeeCombo.getItems().removeIf(item -> item.startsWith("📌 "));

            for (CandidatureExterne c : acceptes) {
                String nomComplet = ((c.getPrenom() != null ? c.getPrenom() : "")
                        + " " + (c.getNom() != null ? c.getNom() : "")).trim();
                if (nomComplet.isEmpty()) continue;

                // Préfixe "📌 " pour les distinguer des employés mock
                String entree = "📌 " + nomComplet;
                if (!employeeCombo.getItems().contains(entree)) {
                    employeeCombo.getItems().add(entree);
                }
            }
        } catch (Exception ex) {
            System.err.println("⚠ Onboarding - chargement candidats acceptés : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Generate (existing — unchanged logic)
    // ─────────────────────────────────────────────────────────────────────
    @FXML
    private void handleGenerate() {
        String employee = employeeCombo.getValue();
        String dept     = departmentCombo.getValue();
        LocalDate date  = arrivalDatePicker.getValue();

        if (employee == null || employee.isBlank()) {
            showAlert("Champ requis", "Veuillez sélectionner un employé.");
            return;
        }
        if (dept == null || dept.isBlank()) {
            showAlert("Champ requis", "Veuillez sélectionner un département.");
            return;
        }
        if (date == null) date = LocalDate.now();

        // Store for AI usage (strip "📌 " prefix if present)
        lastEmployee = employee.startsWith("📌 ") ? employee.substring(3) : employee;
        lastDept     = dept;

        buildTasks(dept);
        renderTaskPanel();
        renderStats();
        renderEmployeeInfo(employee, dept, date);
        renderAutoActions(employee, dept);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Task generation (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void buildTasks(String dept) {
        currentTasks.clear();
        for (String[] t : COMMON_TASKS)
            currentTasks.add(new TaskItem(t[0], t[1], t[2], Integer.parseInt(t[3])));
        List<String[]> extras = DEPT_TASKS.getOrDefault(dept, List.of());
        for (String[] t : extras)
            currentTasks.add(new TaskItem(t[0], t[1], t[2], Integer.parseInt(t[3])));
        currentTasks.sort(Comparator.comparingInt(t -> t.dayOffset));
    }

    // ─────────────────────────────────────────────────────────────────────
    // AI ENHANCE — called when user clicks "🤖 Enrichir avec l'IA"
    // ─────────────────────────────────────────────────────────────────────
    private void handleAiEnhance(Button aiBtn, Label aiStatusLabel) {
        if (lastEmployee == null || lastDept == null) return;

        String[] info = EMPLOYEES.getOrDefault(lastEmployee,
                new String[]{lastDept, "", "Collaborateur"});
        String role = info[2];

        // UI feedback
        aiBtn.setDisable(true);
        aiBtn.setText("⏳ Génération en cours…");
        aiStatusLabel.setVisible(false);

        final String empName = lastEmployee;
        final String empRole = role;
        final String empDept = lastDept;

        Thread thread = new Thread(() -> {
            List<AIService.AiTask> aiTasks =
                    aiService.generateOnboardingTasks(empName, empRole, empDept);

            Platform.runLater(() -> {
                if (aiTasks.isEmpty()) {
                    aiStatusLabel.setText("⚠️ Aucune tâche générée. Vérifiez votre connexion.");
                    aiStatusLabel.setVisible(true);
                } else {
                    // Append AI tasks, avoiding duplicates by label
                    Set<String> existingLabels = new HashSet<>();
                    currentTasks.forEach(t -> existingLabels.add(t.label.toLowerCase()));

                    int added = 0;
                    for (AIService.AiTask t : aiTasks) {
                        if (!existingLabels.contains(t.label.toLowerCase())) {
                            currentTasks.add(new TaskItem(t.icon, t.label, t.category, t.dayOffset, true));
                            added++;
                        }
                    }
                    currentTasks.sort(Comparator.comparingInt(t -> t.dayOffset));

                    renderTaskPanel();
                    renderStats();

                    aiStatusLabel.setText("✅ " + added + " tâche(s) IA ajoutée(s) !");
                    aiStatusLabel.setVisible(true);
                }

                aiBtn.setDisable(false);
                aiBtn.setText("🤖 Enrichir avec l'IA");
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rendering — task panel (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void renderTaskPanel() {
        taskPanel.getChildren().clear();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

        Label title = new Label("✅  Checklist d'intégration");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        long done = currentTasks.stream().filter(t -> t.done).count();
        Label progress = new Label(done + " / " + currentTasks.size() + " complétées");
        progress.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        header.getChildren().addAll(title, spacer, progress);
        taskPanel.getChildren().add(header);

        double pct = currentTasks.isEmpty() ? 0 : (double) done / currentTasks.size();
        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setStyle("-fx-accent: #667eea;");
        VBox.setMargin(progressBar, new Insets(0, 20, 16, 20));
        taskPanel.getChildren().add(progressBar);

        Map<String, List<TaskItem>> byCategory = new LinkedHashMap<>();
        for (TaskItem t : currentTasks) byCategory.computeIfAbsent(t.category, k -> new ArrayList<>()).add(t);

        for (Map.Entry<String, List<TaskItem>> entry : byCategory.entrySet()) {
            Label catLabel = new Label(entry.getKey().toUpperCase());
            catLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; " +
                    "-fx-padding: 10 20 6 20;");
            taskPanel.getChildren().add(catLabel);

            for (TaskItem task : entry.getValue()) {
                HBox row = buildTaskRow(task, progress, pct);
                taskPanel.getChildren().add(row);
                Region sep = new Region();
                sep.setStyle("-fx-background-color: #f8fafc;");
                sep.setPrefHeight(1);
                taskPanel.getChildren().add(sep);
            }
        }
        taskPanel.getChildren().add(new Region());
    }

    private HBox buildTaskRow(TaskItem task, Label progressLabel, double currentPct) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setStyle("-fx-cursor: hand;");

        // AI-generated badge on the row
        if (task.aiGenerated) {
            row.setStyle(row.getStyle() + " -fx-background-color: #f0f4ff;");
        }

        StackPane checkBox = buildCheckCircle(task.done);
        Label icon = new Label(task.icon);
        icon.setStyle("-fx-font-size: 16px;");

        VBox labelBox = new VBox(2);
        Label name = new Label(task.label);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + (task.done ? "#94a3b8" : "#1e293b") + ";");
        if (task.done) name.setStyle(name.getStyle() + " -fx-strikethrough: true;");

        String subText = "J+" + task.dayOffset + "  ·  " + task.category;
        if (task.aiGenerated) subText += "  ·  🤖 IA";
        Label sub = new Label(subText);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        labelBox.getChildren().addAll(name, sub);
        HBox.setHgrow(labelBox, Priority.ALWAYS);

        Label dayBadge = makeBadge("J+" + task.dayOffset,
                task.dayOffset == 0 ? "#dcfce7" : task.dayOffset <= 3 ? "#ede9fe" : "#f1f5f9",
                task.dayOffset == 0 ? "#16a34a" : task.dayOffset <= 3 ? "#667eea" : "#64748b");

        row.getChildren().addAll(checkBox, icon, labelBox, dayBadge);

        row.setOnMouseClicked(e -> {
            task.done = !task.done;
            renderTaskPanel();
        });
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f8fafc; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(task.aiGenerated
                ? "-fx-background-color: #f0f4ff; -fx-cursor: hand;"
                : "-fx-cursor: hand;"));

        return row;
    }

    private StackPane buildCheckCircle(boolean done) {
        StackPane sp = new StackPane();
        Circle c = new Circle(11);
        c.setFill(done ? Color.web("#667eea") : Color.TRANSPARENT);
        c.setStroke(Color.web(done ? "#667eea" : "#cbd5e1"));
        c.setStrokeWidth(2);
        Label check = new Label(done ? "✓" : "");
        check.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        sp.getChildren().addAll(c, check);
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rendering — stats row (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void renderStats() {
        statsRow.getChildren().clear();
        long done     = currentTasks.stream().filter(t -> t.done).count();
        long total    = currentTasks.size();
        long today    = currentTasks.stream().filter(t -> t.dayOffset == 0).count();
        long thisWeek = currentTasks.stream().filter(t -> t.dayOffset <= 7).count();
        long aiCount  = currentTasks.stream().filter(t -> t.aiGenerated).count();

        statsRow.getChildren().addAll(
                statCard("📋", String.valueOf(total),   "Tâches totales",     "#667eea", "#ede9fe"),
                statCard("✅", String.valueOf(done),    "Complétées",         "#16a34a", "#dcfce7"),
                statCard("📅", String.valueOf(today),   "À faire aujourd'hui","#f59e0b", "#fef3c7"),
                statCard("🤖", String.valueOf(aiCount), "Tâches IA",          "#0ea5e9", "#e0f2fe")
        );
    }

    private VBox statCard(String icon, String value, String label, String fg, String bg) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");
        card.setPrefWidth(180);

        HBox iconRow = new HBox(8);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        iconRow.getChildren().add(iconLbl);

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(iconRow, valueLbl, labelLbl);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rendering — employee info card (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void renderEmployeeInfo(String name, String dept, LocalDate date) {
        welcomeCard.getChildren().clear();

        Label title = new Label("👤  Informations employé");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        welcomeCard.getChildren().add(title);

        // Retirer le préfixe "📌 " pour l'affichage
        String displayName = name.startsWith("📌 ") ? name.substring(3) : name;
        boolean isFromDB   = name.startsWith("📌 ");

        StackPane avatar = new StackPane();
        Circle bg = new Circle(30);
        bg.setFill(Color.web(isFromDB ? "#dcfce7" : "#ede9fe")); // vert pour candidats DB
        Label initials = new Label(getInitials(displayName));
        initials.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                + (isFromDB ? "#059669" : "#667eea") + ";");
        avatar.getChildren().addAll(bg, initials);

        Label nameLbl = new Label(displayName);
        nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        // Chercher d'abord dans les employés mock, sinon utiliser les données DB
        String[] info = EMPLOYEES.getOrDefault(displayName,
                EMPLOYEES.getOrDefault(name,
                        new String[]{dept, displayName.toLowerCase().replace(" ", ".") + "@humania.com",
                                isFromDB ? "Nouveau collaborateur (Candidature acceptée)" : "Nouveau collaborateur"}));
        Label roleLbl = new Label(info[2]);
        roleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isFromDB ? "#059669" : "#667eea") + ";");

        HBox nameRow = new HBox(12, avatar, new VBox(2, nameLbl, roleLbl));
        nameRow.setAlignment(Pos.CENTER_LEFT);
        welcomeCard.getChildren().add(nameRow);

        // Badge "Nouveau candidat" si vient de la DB de recrutement
        if (isFromDB) {
            Label dbBadge = makeBadge("🆕 Candidature acceptée", "#d1fae5", "#059669");
            dbBadge.setStyle(dbBadge.getStyle() + " -fx-font-size: 11px;");
            welcomeCard.getChildren().add(dbBadge);
        }

        Region div = new Region(); div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #f1f5f9;");
        welcomeCard.getChildren().add(div);

        String email = info[1].isEmpty()
                ? displayName.toLowerCase().replace(" ", ".") + "@humania.com"
                : info[1];
        welcomeCard.getChildren().addAll(
                infoRow("🏢", "Département", dept),
                infoRow("📧", "Email", email),
                infoRow("📅", "Date d'arrivée", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                infoRow("🎯", "Tâches générées", currentTasks.size() + " étapes")
        );

        Label statusBadge = makeBadge("● En cours d'intégration", "#dcfce7", "#16a34a");
        statusBadge.setStyle(statusBadge.getStyle() + " -fx-font-size: 12px;");
        welcomeCard.getChildren().add(statusBadge);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rendering — auto-actions card  ← AI button added here
    // ─────────────────────────────────────────────────────────────────────
    private void renderAutoActions(String employee, String dept) {
        autoActionsCard.getChildren().clear();

        Label title = new Label("⚙️  Actions automatiques");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        autoActionsCard.getChildren().add(title);

        Label sub = new Label("Déclenchées automatiquement à la génération du parcours");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");
        autoActionsCard.getChildren().add(sub);

        String email = EMPLOYEES.getOrDefault(employee,
                new String[]{"", employee.toLowerCase().replace(" ", ".") + "@humania.com", ""})[1];
        if (email.isBlank()) email = employee.toLowerCase().replace(" ", ".") + "@humania.com";

        List<String[]> actions = new ArrayList<>();
        actions.add(new String[]{"✉️", "Email de bienvenue envoyé",      "à " + email,                      "#dcfce7", "#16a34a"});
        actions.add(new String[]{"🔐", "Compte utilisateur créé",        "login: " + email.split("@")[0],    "#e0f2fe", "#0ea5e9"});
        actions.add(new String[]{"🪪", "Badge programmé",                "Valide à partir de J+0",           "#fef3c7", "#f59e0b"});
        actions.add(new String[]{"📋", "Dossier RH initialisé",          "Pièces manquantes à compléter",    "#ede9fe", "#667eea"});
        if (dept.equals("Ingénierie") || dept.equals("RH") || dept.equals("Finance")) {
            actions.add(new String[]{"💻", "Accès VPN configuré",        "Identifiants envoyés par email",   "#f1f5f9", "#475569"});
        }
        actions.add(new String[]{"📅", "Agenda J+1 planifié",            "Réunion d'accueil avec manager",   "#dcfce7", "#16a34a"});

        for (String[] a : actions) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: " + a[3] + "; -fx-background-radius: 8;");
            Label ic = new Label(a[0]); ic.setStyle("-fx-font-size: 15px;");
            VBox txt = new VBox(1);
            Label lbl = new Label(a[1]);
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + a[4] + ";");
            Label det = new Label(a[2]);
            det.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            txt.getChildren().addAll(lbl, det);
            Label ok = new Label("✓");
            ok.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + a[4] + ";");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(ic, txt, sp, ok);
            autoActionsCard.getChildren().add(row);
        }

        // ── Existing buttons ──────────────────────────────────────────────
        Button markAll = new Button("✅  Tout marquer complété");
        markAll.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold;" +
                " -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        markAll.setMaxWidth(Double.MAX_VALUE);
        markAll.setOnAction(e -> {
            currentTasks.forEach(t -> t.done = true);
            renderTaskPanel();
            renderStats();
        });

        Button resetBtn = new Button("↺  Réinitialiser");
        resetBtn.setStyle("-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-size: 12px;" +
                " -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: #e2e8f0;" +
                " -fx-border-radius: 8; -fx-cursor: hand;");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            currentTasks.forEach(t -> t.done = false);
            renderTaskPanel();
            renderStats();
        });

        // ── 🤖 AI button + status label ───────────────────────────────────
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");
        VBox.setMargin(divider, new Insets(6, 0, 6, 0));

        Button aiBtn = new Button("🤖 Enrichir avec l'IA");
        aiBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2);" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-padding: 9 16; -fx-background-radius: 8; -fx-cursor: hand;");
        aiBtn.setMaxWidth(Double.MAX_VALUE);

        Label aiStatusLabel = new Label("");
        aiStatusLabel.setVisible(false);
        aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #667eea; -fx-font-style: italic;");
        aiStatusLabel.setWrapText(true);

        aiBtn.setOnAction(e -> handleAiEnhance(aiBtn, aiStatusLabel));

        autoActionsCard.getChildren().addAll(markAll, resetBtn, divider, aiBtn, aiStatusLabel);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Empty state (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private void showEmptyState() {
        taskPanel.getChildren().clear();
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        Label ico = new Label("🚀");
        ico.setStyle("-fx-font-size: 48px;");
        Label msg = new Label("Sélectionnez un employé et cliquez sur\n\"Générer le parcours\" pour commencer.");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center; -fx-alignment: center;");
        msg.setWrapText(true);
        empty.getChildren().addAll(ico, msg);
        taskPanel.getChildren().add(empty);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers (unchanged)
    // ─────────────────────────────────────────────────────────────────────
    private HBox infoRow(String icon, String key, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 13px;");
        Label k  = new Label(key + " :"); k.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-min-width: 100;");
        Label v  = new Label(value); v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        row.getChildren().addAll(ic, k, v);
        return row;
    }

    private Label makeBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0);
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Inner model — added aiGenerated flag
    // ─────────────────────────────────────────────────────────────────────
    private static class TaskItem {
        final String icon, label, category;
        final int    dayOffset;
        boolean      done        = false;
        boolean      aiGenerated = false;

        TaskItem(String icon, String label, String category, int dayOffset) {
            this.icon      = icon;
            this.label     = label;
            this.category  = category;
            this.dayOffset = dayOffset;
        }

        /** Constructor for AI-generated tasks. */
        TaskItem(String icon, String label, String category, int dayOffset, boolean aiGenerated) {
            this(icon, label, category, dayOffset);
            this.aiGenerated = aiGenerated;
        }
    }
}