package utilisateur.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utils.MyDataBase;
import utils.Session;
import utils.UserSession;

import java.sql.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Dashboard d'accueil HUMANIA — design moderne, desktop-first.
 * Palette exactement synchronisée avec MainFX/sidebar :
 *   BG #f0f4fb · Rouge #e57373 · Bleu #5b8dee · Amber #f5c842 · Vert #5db87a
 */
public class HomeDashboardController {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    // ── Palette synchronisée avec MainFX ─────────────────────────────────
    private static final String BG       = "#f0f4fb";
    private static final String WHITE    = "#ffffff";
    private static final String DARK     = "#1e293b";
    private static final String MID      = "#334155";
    private static final String MUTED    = "#64748b";
    private static final String BORDER   = "#e2e8f0";
    private static final String SURFACE  = "#f8fafc";

    // Couleurs HUMANIA (4 dots sidebar)
    private static final String RED      = "#e57373";
    private static final String RED_L    = "#fde8e8";
    private static final String BLUE     = "#5b8dee";
    private static final String BLUE_L   = "#e3eeff";
    private static final String AMBER    = "#f5c842";
    private static final String AMBER_L  = "#fff8e1";
    private static final String GREEN    = "#5db87a";
    private static final String GREEN_L  = "#e6f4ea";
    private static final String PURPLE   = "#8b5cf6";
    private static final String PURPLE_L = "#f5f3ff";
    private static final String ORANGE   = "#f97316";
    private static final String ORANGE_L = "#fff7ed";

    // ── Point d'entrée ───────────────────────────────────────────────────
    public ScrollPane buildView() {
        Utilisateur u = Session.getUtilisateurConnecte();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + BG + ";");
        root.getChildren().add(buildHero(u));
        root.getChildren().add(buildBody(u));

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + "; -fx-border-color: transparent;");

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), root);
        ft.setToValue(1); ft.play();
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HERO — bande couleurs + salutation
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildHero(Utilisateur u) {
        VBox wrapper = new VBox(0);

        // Bande 4 couleurs HUMANIA (comme la scrollbar et la user card)
        HBox colorBar = new HBox(0);
        colorBar.setMinHeight(4); colorBar.setPrefHeight(4);
        for (String col : new String[]{RED, BLUE, AMBER, GREEN}) {
            Region seg = new Region();
            seg.setPrefHeight(4);
            HBox.setHgrow(seg, Priority.ALWAYS);
            seg.setStyle("-fx-background-color: " + col + ";");
            colorBar.getChildren().add(seg);
        }

        // Contenu hero
        HBox heroContent = new HBox(0);
        heroContent.setAlignment(Pos.CENTER_LEFT);
        heroContent.setMinHeight(148); heroContent.setPrefHeight(148);
        heroContent.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-padding: 0 52 0 52;" +
                        "-fx-border-color: transparent transparent " + BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        heroContent.setEffect(new DropShadow(10, 0, 3, Color.rgb(0, 20, 80, 0.04)));

        // ── Zone texte ────────────────────────────────────────────────────
        VBox textZone = new VBox(7);
        textZone.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textZone, Priority.ALWAYS);

        // Rôle + Date
        HBox contextRow = new HBox(12);
        contextRow.setAlignment(Pos.CENTER_LEFT);
        String roleStr = u != null && u.getRole() != null ? u.getRole().name() : "EMPLOYÉ";
        Label rolePill = roleBadge(roleStr);
        LocalDate today = LocalDate.now();
        String dateTxt = cap(today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH))
                + " " + today.getDayOfMonth() + " "
                + cap(today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH))
                + " " + today.getYear();
        Label dateLbl = new Label("📅  " + dateTxt);
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");
        contextRow.getChildren().addAll(rolePill, dateLbl);

        // Salutation
        String firstName = u != null && u.getPrenom() != null ? u.getPrenom() : "Collaborateur";
        Label greetLbl = new Label(getGreeting() + ", " + firstName + " 👋");
        greetLbl.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 30px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + DARK + ";"
        );

        // Sous-titre poste/département
        String poste = u != null && u.getPosteActuel() != null ? u.getPosteActuel() : "";
        String dept  = u != null && u.getDepartement() != null ? u.getDepartement() : "";
        String sub   = (poste.isBlank() && dept.isBlank()) ? "Bienvenue dans votre espace RH"
                : (!poste.isBlank() && !dept.isBlank()) ? poste + "  ·  " + dept
                : poste + dept;
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + ";");

        textZone.getChildren().addAll(contextRow, greetLbl, subLbl);

        // ── Avatar card droite ────────────────────────────────────────────
        VBox avatarZone = buildHeroAvatar(u);

        heroContent.getChildren().addAll(textZone, avatarZone);

        // Animations
        textZone.setTranslateX(-22); textZone.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(80),
                        new KeyValue(textZone.translateXProperty(), -22),
                        new KeyValue(textZone.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(460),
                        new KeyValue(textZone.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(textZone.opacityProperty(), 1, Interpolator.EASE_IN))
        ).play();

        wrapper.getChildren().addAll(colorBar, heroContent);
        return wrapper;
    }

    private VBox buildHeroAvatar(Utilisateur u) {
        VBox zone = new VBox(8);
        zone.setAlignment(Pos.CENTER);
        zone.setPadding(new Insets(16, 24, 16, 32));

        StackPane av = buildAvatar(u, 60, 18, 22);

        String fullName = u != null
                ? ((u.getPrenom() != null ? u.getPrenom() : "") + " " +
                (u.getNom()    != null ? u.getNom()    : "")).trim()
                : "—";
        Label nameLbl = new Label(fullName.isEmpty() ? "—" : fullName);
        nameLbl.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: " + DARK + ";");

        HBox online = new HBox(5); online.setAlignment(Pos.CENTER);
        Circle dotC = new Circle(4); dotC.setFill(Color.web(GREEN));
        dotC.setEffect(new DropShadow(6, Color.web(GREEN)));
        Label onlineLbl = new Label("En ligne");
        onlineLbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
        online.getChildren().addAll(dotC, onlineLbl);

        zone.getChildren().addAll(av, nameLbl, online);

        zone.setTranslateX(22); zone.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(zone.translateXProperty(), 22), new KeyValue(zone.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(540),
                        new KeyValue(zone.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(zone.opacityProperty(), 1, Interpolator.EASE_IN))
        ).play();
        return zone;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BODY
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildBody(Utilisateur u) {
        VBox body = new VBox(28);
        body.setPadding(new Insets(30, 48, 52, 48));
        body.getChildren().add(buildKpiRow(u));
        body.getChildren().add(buildMainRow(u));
        body.getChildren().add(buildModuleRow(u));
        return body;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // KPI ROW — stats selon le rôle
    // ═══════════════════════════════════════════════════════════════════════
    private HBox buildKpiRow(Utilisateur u) {
        HBox row = new HBox(14);
        if (u == null) return row;

        int uid   = u.getId();
        String em = u.getEmail() != null ? u.getEmail() : "";
        boolean isAdmin = u.getRole() == Role.ADMIN;

        if (isAdmin) {
            // ADMIN — stats globales de supervision
            int totalUsers  = queryInt("SELECT COUNT(*) FROM utilisateur WHERE statut='Actif'");
            int candidats   = queryInt("SELECT COUNT(*) FROM utilisateur WHERE role='CANDIDAT' AND statut='Actif'");
            int postesOuv   = queryInt("SELECT COUNT(*) FROM poste WHERE statut='Ouvert'");
            int reunions    = queryInt("SELECT COUNT(*) FROM reunion WHERE dateHeureDebut > NOW()");
            int congesWait  = queryInt("SELECT COUNT(*) FROM conge WHERE statut='En attente'");
            row.getChildren().add(kpi("👥", "Collaborateurs\nactifs",  totalUsers,  BLUE,   BLUE_L));
            row.getChildren().add(kpi("📋", "Candidats\nen cours",     candidats,   RED,    RED_L));
            row.getChildren().add(kpi("💼", "Postes\nouvert",          postesOuv,   GREEN,  GREEN_L));
            row.getChildren().add(kpi("📆", "Réunions\nplanifiées",    reunions,    PURPLE, PURPLE_L));
            row.getChildren().add(kpi("⏳", "Congés\nen attente",      congesWait,  AMBER,  AMBER_L));
        } else {
            // Autres rôles — données personnelles
            int conges   = queryInt("SELECT COUNT(*) FROM conge WHERE utilisateur_id=? AND statut='En attente'", uid);
            int absences = queryInt("SELECT COUNT(*) FROM absence WHERE utilisateur_id=? AND MONTH(date_debut)=MONTH(CURDATE()) AND YEAR(date_debut)=YEAR(CURDATE())", uid);
            int comps    = queryInt("SELECT COUNT(*) FROM competenceemploye WHERE employe_id=? AND niveauValide=1", uid);
            int reunions = queryInt("SELECT COUNT(*) FROM reunion WHERE dateHeureDebut > NOW() AND JSON_CONTAINS(participants, JSON_QUOTE(?), '$')", em);
            row.getChildren().add(kpi("📋", "Congés\nen attente",    conges,   BLUE,   BLUE_L));
            row.getChildren().add(kpi("📅", "Absences\nce mois",     absences, AMBER,  AMBER_L));
            row.getChildren().add(kpi("🎯", "Compétences\nvalidées", comps,    GREEN,  GREEN_L));
            row.getChildren().add(kpi("📆", "Réunions\nà venir",     reunions, PURPLE, PURPLE_L));
            if (u.getRole() == Role.MANAGER) {
                int actifs = queryInt("SELECT COUNT(*) FROM utilisateur WHERE statut='Actif'");
                row.getChildren().add(kpi("👥", "Équipe\nactive", actifs, RED, RED_L));
            }
        }

        for (javafx.scene.Node n : row.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // Stagger animation
        int[] i = {0};
        for (javafx.scene.Node n : row.getChildren()) {
            n.setOpacity(0); n.setTranslateY(14);
            int delay = i[0] * 70;
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(n.opacityProperty(), 0), new KeyValue(n.translateYProperty(), 14)),
                    new KeyFrame(Duration.millis(delay + 360),
                            new KeyValue(n.opacityProperty(), 1, Interpolator.EASE_IN),
                            new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_OUT))
            );
            tl.setDelay(Duration.millis(250)); tl.play();
            i[0]++;
        }
        return row;
    }

    private VBox kpi(String icon, String label, int value, String accent, String lightBg) {
        VBox card = new VBox(0);
        card.setMinWidth(0);
        card.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.07), 14, 0, 0, 3);" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 16; -fx-border-width: 1;"
        );

        // Barre accent top
        Region topBar = new Region(); topBar.setPrefHeight(4); topBar.setMinHeight(4);
        topBar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 16 16 0 0;");

        VBox inner = new VBox(10); inner.setPadding(new Insets(15, 20, 17, 20));

        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(44, 44); iconBadge.setMinSize(44, 44); iconBadge.setMaxSize(44, 44);
        Region iconBg = new Region(); iconBg.setPrefSize(44, 44);
        iconBg.setStyle("-fx-background-color: " + lightBg + "; -fx-background-radius: 12;");
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 19px;");
        iconBadge.getChildren().addAll(iconBg, iconLbl);

        Label valueLbl = new Label(String.valueOf(value));
        valueLbl.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 34px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + DARK + ";"
        );

        HBox topRow = new HBox(12); topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(iconBadge, valueLbl);

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + MUTED + ";");
        labelLbl.setWrapText(true);

        Region accentLine = new Region(); accentLine.setPrefHeight(2); accentLine.setMaxHeight(2);
        accentLine.setPrefWidth(24); accentLine.setMaxWidth(24);
        accentLine.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 1;");

        inner.getChildren().addAll(topRow, labelLbl, accentLine);
        card.getChildren().addAll(topBar, inner);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + lightBg + "; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, " + accent + "55, 20, 0, 0, 6);" +
                        "-fx-translate-y: -3; -fx-cursor: hand;" +
                        "-fx-border-color: " + accent + "44; -fx-border-radius: 16; -fx-border-width: 1;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + WHITE + "; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.07), 14, 0, 0, 3);" +
                        "-fx-translate-y: 0;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 16; -fx-border-width: 1;"
        ));
        animateCount(valueLbl, value);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN ROW — 3 colonnes
    // ═══════════════════════════════════════════════════════════════════════
    private HBox buildMainRow(Utilisateur u) {
        HBox row = new HBox(16);

        VBox profileCard = buildProfileCard(u);
        profileCard.setMinWidth(255); profileCard.setMaxWidth(280);

        VBox activityCard = buildActivityCard(u);
        HBox.setHgrow(activityCard, Priority.ALWAYS);

        VBox rightCol = new VBox(16);
        rightCol.setMinWidth(235); rightCol.setMaxWidth(262);
        rightCol.getChildren().addAll(buildMeetingCard(u), buildLeaveCard(u));

        row.getChildren().addAll(profileCard, activityCard, rightCol);
        return row;
    }

    // ── Profil ─────────────────────────────────────────────────────────────
    private VBox buildProfileCard(Utilisateur u) {
        VBox card = card();
        card.getChildren().addAll(cardHeader("👤", "Mon Profil", BLUE), divider());
        if (u == null) { card.getChildren().add(muted("—")); return card; }

        VBox avBox = new VBox(10); avBox.setAlignment(Pos.CENTER);
        avBox.setPadding(new Insets(14, 0, 12, 0));

        StackPane av = buildAvatar(u, 72, 22, 26);
        String fullName = ((u.getPrenom() != null ? u.getPrenom() : "") + " " +
                (u.getNom()    != null ? u.getNom()    : "")).trim();
        Label nameLbl = new Label(fullName.isEmpty() ? "—" : fullName);
        nameLbl.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: " + DARK + ";");

        Label roleLbl = roleBadge(u.getRole() != null ? u.getRole().name() : "—");

        HBox online = new HBox(5); online.setAlignment(Pos.CENTER);
        Circle dot = new Circle(4); dot.setFill(Color.web(GREEN)); dot.setEffect(new DropShadow(6, Color.web(GREEN)));
        Label onlineLbl = new Label("En ligne");
        onlineLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
        online.getChildren().addAll(dot, onlineLbl);

        avBox.getChildren().addAll(av, nameLbl, roleLbl, online);
        card.getChildren().addAll(avBox, divider());

        VBox infos = new VBox(6); infos.setPadding(new Insets(8, 0, 0, 0));
        if (u.getEmail()        != null) infos.getChildren().add(infoRow("📧", u.getEmail(),           DARK,  BLUE_L));
        if (u.getUsername()     != null) infos.getChildren().add(infoRow("🔑", "@" + u.getUsername(),  BLUE,  BLUE_L));
        if (u.getPosteActuel()  != null) infos.getChildren().add(infoRow("💼", u.getPosteActuel(),     DARK,  GREEN_L));
        if (u.getDepartement()  != null) infos.getChildren().add(infoRow("🏢", u.getDepartement(),     DARK,  AMBER_L));
        if (u.getNumtel()       != null) infos.getChildren().add(infoRow("📞", u.getNumtel(),          DARK,  PURPLE_L));
        if (u.getMatricule()    != null) infos.getChildren().add(infoRow("#", u.getMatricule(),       MUTED, SURFACE));
        if (u.getDateEmbauche() != null) infos.getChildren().add(infoRow("🗓",  "Depuis " + u.getDateEmbauche(), MUTED, SURFACE));
        card.getChildren().add(infos);
        return card;
    }

    // ── Activité récente ──────────────────────────────────────────────────
    private VBox buildActivityCard(Utilisateur u) {
        VBox card = card();
        HBox.setHgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(cardHeader("⚡", "Activité récente", AMBER), divider());
        if (u == null) return card;

        VBox list = new VBox(0); list.setPadding(new Insets(4, 0, 0, 0));

        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT a.date_debut, a.statut, ta.libelle FROM absence a " +
                            "LEFT JOIN type_absence ta ON a.type_absence_id = ta.id " +
                            "WHERE a.utilisateur_id=? ORDER BY a.date_debut DESC LIMIT 4");
            ps.setInt(1, u.getId()); ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("libelle");
                list.getChildren().add(activityRow("📅",
                        (type != null ? type : "Absence") + "  ·  " + rs.getString("date_debut"),
                        rs.getString("statut")));
            }
        } catch (Exception ignored) {}

        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT c.date_debut, c.date_fin, c.statut, tc.libelle FROM conge c " +
                            "LEFT JOIN type_conge tc ON c.type_conge_id = tc.id " +
                            "WHERE c.utilisateur_id=? ORDER BY c.date_debut DESC LIMIT 3");
            ps.setInt(1, u.getId()); ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("libelle");
                list.getChildren().add(activityRow("🌴",
                        (type != null ? type : "Congé") + "  ·  " + rs.getString("date_debut") + " → " + rs.getString("date_fin"),
                        rs.getString("statut")));
            }
        } catch (Exception ignored) {}

        if (list.getChildren().isEmpty()) list.getChildren().add(emptyState("📭", "Aucune activité récente"));
        card.getChildren().add(list);
        VBox.setVgrow(list, Priority.ALWAYS);
        return card;
    }

    private HBox activityRow(String icon, String text, String statut) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(11, 8, 11, 8));
        row.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        String[] ss = statusStyle(statut);

        StackPane dotPane = new StackPane();
        dotPane.setPrefSize(8, 8); dotPane.setMinSize(8, 8); dotPane.setMaxSize(8, 8);
        Circle dotC = new Circle(4); dotC.setFill(Color.web(ss[0]));
        dotPane.getChildren().add(dotC);

        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 14px;");
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MID + ";");
        textLbl.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(textLbl, Priority.ALWAYS);

        Label badge = new Label(statut != null ? statut : "—");
        badge.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + ss[0] + ";" +
                        "-fx-background-color: " + ss[1] + "; -fx-padding: 3 9 3 9; -fx-background-radius: 20;"
        );
        row.getChildren().addAll(dotPane, iconLbl, textLbl, badge);
        return row;
    }

    // ── Prochaine réunion ─────────────────────────────────────────────────
    private VBox buildMeetingCard(Utilisateur u) {
        VBox card = card();
        card.getChildren().addAll(cardHeader("📆", "Prochaine réunion", PURPLE), divider());
        if (u == null || u.getEmail() == null) { card.getChildren().add(emptyState("📭", "Aucune réunion planifiée")); return card; }

        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT titre, dateHeureDebut FROM reunion WHERE dateHeureDebut > NOW() " +
                            "AND JSON_CONTAINS(participants, JSON_QUOTE(?), '$') ORDER BY dateHeureDebut ASC LIMIT 1");
            ps.setString(1, u.getEmail()); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LocalDateTime ldt = rs.getTimestamp("dateHeureDebut").toLocalDateTime();
                String timeStr = String.format("%s %d %s  %02d:%02d",
                        cap(ldt.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH)),
                        ldt.getDayOfMonth(),
                        cap(ldt.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH)),
                        ldt.getHour(), ldt.getMinute());

                VBox inner = new VBox(10); inner.setPadding(new Insets(12, 0, 0, 0));

                HBox timeBadge = new HBox(7); timeBadge.setAlignment(Pos.CENTER_LEFT);
                timeBadge.setStyle(
                        "-fx-background-color: " + PURPLE_L + "; -fx-background-radius: 10;" +
                                "-fx-padding: 7 12 7 12; -fx-border-color: " + PURPLE + "33; -fx-border-radius: 10; -fx-border-width: 1;"
                );
                Label clockIco = new Label("🕐"); clockIco.setStyle("-fx-font-size: 13px;");
                Label timeLbl = new Label(timeStr);
                timeLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + PURPLE + ";");
                timeBadge.getChildren().addAll(clockIco, timeLbl);

                Label titleLbl = new Label(rs.getString("titre"));
                titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + DARK + "; -fx-wrap-text: true;");
                titleLbl.setMaxWidth(Double.MAX_VALUE);

                inner.getChildren().addAll(timeBadge, titleLbl);
                card.getChildren().add(inner);
            } else {
                card.getChildren().add(emptyState("📭", "Aucune réunion à venir"));
            }
        } catch (Exception e) { card.getChildren().add(muted("—")); }
        return card;
    }

    // ── Solde de congés ───────────────────────────────────────────────────
    private VBox buildLeaveCard(Utilisateur u) {
        VBox card = card();
        card.getChildren().addAll(cardHeader("🌴", "Solde de congés", GREEN), divider());
        if (u == null) { card.getChildren().add(muted("—")); return card; }

        int approuves = queryInt("SELECT COALESCE(SUM(nbr_jours),0) FROM conge WHERE utilisateur_id=? AND statut='Approuvé'", u.getId());
        int enAttente = queryInt("SELECT COUNT(*) FROM conge WHERE utilisateur_id=? AND statut='En attente'", u.getId());

        VBox inner = new VBox(12); inner.setPadding(new Insets(12, 0, 0, 0));

        HBox stats = new HBox(8);
        stats.setAlignment(Pos.CENTER);
        VBox approvedBox = leaveBlock(approuves + " j", "Approuvés", GREEN, GREEN_L, true);
        VBox waitBox     = leaveBlock(String.valueOf(enAttente), "En attente", AMBER, AMBER_L, false);
        HBox.setHgrow(approvedBox, Priority.ALWAYS);
        HBox.setHgrow(waitBox, Priority.ALWAYS);
        stats.getChildren().addAll(approvedBox, waitBox);

        StackPane progressBar = new StackPane(); progressBar.setAlignment(Pos.CENTER_LEFT);
        Region bgBar = new Region(); bgBar.setPrefHeight(7);
        bgBar.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4;");
        Region fillBar = new Region(); fillBar.setPrefHeight(7); fillBar.setPrefWidth(0);
        fillBar.setStyle("-fx-background-color: linear-gradient(to right, " + GREEN + ", " + BLUE + "); -fx-background-radius: 4;");
        StackPane.setAlignment(bgBar, Pos.CENTER_LEFT);
        StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);
        progressBar.getChildren().addAll(bgBar, fillBar);

        inner.getChildren().addAll(stats, progressBar);
        card.getChildren().add(inner);

        int total = Math.max(approuves + enAttente, 1);
        double pct = (double) approuves / total;
        Platform.runLater(() -> {
            double w = progressBar.getWidth();
            if (w > 10) {
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(fillBar.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(800), new KeyValue(fillBar.prefWidthProperty(), w * pct, Interpolator.EASE_OUT))
                );
                tl.setDelay(Duration.millis(600)); tl.play();
            }
        });
        return card;
    }

    private VBox leaveBlock(String val, String lbl, String accent, String bg, boolean leftRounded) {
        VBox box = new VBox(3); box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12, 14, 12, 14));
        String r = leftRounded ? "10 0 0 10" : "0 10 10 0";
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: " + r + ";");
        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        Label lblLbl = new Label(lbl);
        lblLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + MUTED + ";");
        box.getChildren().addAll(valLbl, lblLbl);
        return box;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODULE ROW — accès rapide
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildModuleRow(Utilisateur u) {
        VBox section = new VBox(16);

        // Titre avec barre accent
        HBox titleRow = new HBox(0); titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBlock = new VBox(3);
        HBox titleLeft = new HBox(10); titleLeft.setAlignment(Pos.CENTER_LEFT);
        Region accentBar = new Region();
        accentBar.setPrefWidth(4); accentBar.setPrefHeight(22);
        accentBar.setStyle("-fx-background-color: " + BLUE + "; -fx-background-radius: 2;");
        Label title = new Label("Accès rapide");
        title.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 16px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + DARK + ";"
        );
        titleLeft.getChildren().addAll(accentBar, title);
        Label subTitle = new Label("Naviguez directement vers vos modules");
        subTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + "; -fx-padding: 0 0 0 14;");
        titleBlock.getChildren().addAll(titleLeft, subTitle);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox hint = new HBox(5); hint.setAlignment(Pos.CENTER);
        Label hIco = new Label("👆"); hIco.setStyle("-fx-font-size: 12px;");
        Label hLbl = new Label("Cliquez pour naviguer");
        hLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");
        hint.getChildren().addAll(hIco, hLbl);
        titleRow.getChildren().addAll(titleBlock, sp, hint);

        // Modules selon rôle
        boolean isAdmin = u != null && u.getRole() == Role.ADMIN;
        Object[][] mods = isAdmin
                ? new Object[][]{
                {"👥", "Utilisateurs",  BLUE,   BLUE_L,   test.MainFX.GESTION_UTILISATEUR},
                {"💼", "Recrutement",   ORANGE, ORANGE_L, test.MainFX.PAGE_CANDIDATURE_INTERNE},
                {"💬", "Social Media",  RED,    RED_L,    null},
                {"🎯", "Compétences",   GREEN,  GREEN_L,  test.MainFX.COMPETENCY_CATALOG},
                {"📆", "Réunions",      PURPLE, PURPLE_L, test.MainFX.PAGE_REUNION},
                {"📌", "Planification", AMBER,  AMBER_L,  test.MainFX.PAGE_RESERVER_ESPACES},
        }
                : new Object[][]{
                {"💼", "Recrutement",   ORANGE, ORANGE_L, test.MainFX.PAGE_CANDIDATURE_INTERNE},
                {"💬", "Social Media",  BLUE,   BLUE_L,   null},
                {"🎯", "Compétences",   GREEN,  GREEN_L,  test.MainFX.COMPETENCY_CATALOG},
                {"🌴", "Congés",        AMBER,  AMBER_L,  test.MainFX.ABSENCES_CONGES_VIEW},
                {"📆", "Réunions",      PURPLE, PURPLE_L, test.MainFX.PAGE_REUNION},
                {"📌", "Planification", RED,    RED_L,    test.MainFX.PAGE_RESERVER_ESPACES},
        };

        HBox tiles = new HBox(14);
        int[] ai = {0};
        for (Object[] m : mods) {
            VBox tile = moduleTile((String)m[0], (String)m[1], (String)m[2], (String)m[3], (String)m[4]);
            HBox.setHgrow(tile, Priority.ALWAYS);
            tile.setOpacity(0); tile.setTranslateY(12);
            int d = ai[0] * 60;
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(d),
                            new KeyValue(tile.opacityProperty(), 0), new KeyValue(tile.translateYProperty(), 12)),
                    new KeyFrame(Duration.millis(d + 320),
                            new KeyValue(tile.opacityProperty(), 1, Interpolator.EASE_IN),
                            new KeyValue(tile.translateYProperty(), 0, Interpolator.EASE_OUT))
            );
            tl.setDelay(Duration.millis(500)); tl.play();
            tiles.getChildren().add(tile);
            ai[0]++;
        }
        section.getChildren().addAll(titleRow, tiles);
        return section;
    }

    private VBox moduleTile(String icon, String label, String accent, String bgLight, String path) {
        VBox tile = new VBox(12);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(22, 12, 20, 12));
        tile.setMinWidth(0);
        tile.setStyle(
                "-fx-background-color: " + WHITE + "; -fx-background-radius: 18; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.06), 12, 0, 0, 3);" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1;"
        );

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(56, 56); iconWrap.setMinSize(56, 56); iconWrap.setMaxSize(56, 56);
        Region iconBg = new Region(); iconBg.setPrefSize(56, 56);
        iconBg.setStyle(
                "-fx-background-color: " + bgLight + "; -fx-background-radius: 16;" +
                        "-fx-border-color: " + accent + "33; -fx-border-radius: 16; -fx-border-width: 1.5;"
        );
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 24px;");
        iconWrap.getChildren().addAll(iconBg, iconLbl);

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + DARK + "; -fx-text-alignment: center;");
        nameLbl.setWrapText(true); nameLbl.setTextAlignment(TextAlignment.CENTER); nameLbl.setMaxWidth(Double.MAX_VALUE);

        Region accentLine = new Region(); accentLine.setPrefHeight(3); accentLine.setMaxHeight(3);
        accentLine.setPrefWidth(30); accentLine.setMaxWidth(30);
        accentLine.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 2;");

        tile.getChildren().addAll(iconWrap, nameLbl, accentLine);

        tile.setOnMouseEntered(e -> {
            tile.setStyle(
                    "-fx-background-color: " + bgLight + "; -fx-background-radius: 18; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, " + accent + "77, 22, 0, 0, 8); -fx-translate-y: -4;" +
                            "-fx-border-color: " + accent + "55; -fx-border-radius: 18; -fx-border-width: 1.5;"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(130), iconWrap);
            st.setToX(1.1); st.setToY(1.1); st.play();
        });
        tile.setOnMouseExited(e -> {
            tile.setStyle(
                    "-fx-background-color: " + WHITE + "; -fx-background-radius: 18; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.06), 12, 0, 0, 3); -fx-translate-y: 0;" +
                            "-fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1;"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(130), iconWrap);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        tile.setOnMouseClicked(e -> {
            test.MainFX app = test.MainFX.getInstance();
            if (app == null) return;
            if (path == null) app.navigateTo(test.MainFX.SOCIAL_FEED_VIEW);
            else              app.navigateTo(path);
        });
        return tile;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMPOSANTS RÉUTILISABLES
    // ═══════════════════════════════════════════════════════════════════════
    private StackPane buildAvatar(Utilisateur u, double size, double radius, double fontSize) {
        StackPane av = new StackPane();
        av.setPrefSize(size, size); av.setMinSize(size, size); av.setMaxSize(size, size);
        Region avBg = new Region(); avBg.setPrefSize(size, size);
        avBg.setStyle(
                "-fx-background-color: #dbeafe; -fx-background-radius: " + radius + ";" +
                        "-fx-border-color: #93c5fd; -fx-border-radius: " + radius + "; -fx-border-width: 1.5;"
        );
        String init = "";
        if (u != null) {
            if (u.getPrenom() != null && !u.getPrenom().isEmpty()) init += u.getPrenom().charAt(0);
            if (u.getNom()    != null && !u.getNom().isEmpty())    init += u.getNom().charAt(0);
        }
        Label initLbl = new Label(init.isEmpty() ? "?" : init.toUpperCase());
        initLbl.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: " + (int)fontSize + "px;" +
                        "-fx-font-weight: bold; -fx-text-fill: #1d4ed8;"
        );
        av.getChildren().addAll(avBg, initLbl);
        return av;
    }

    private VBox card() {
        VBox c = new VBox(12);
        c.setPadding(new Insets(20, 22, 22, 22));
        c.setStyle(
                "-fx-background-color: " + WHITE + "; -fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.06), 16, 0, 0, 4);" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1;"
        );
        return c;
    }

    private HBox cardHeader(String icon, String title, String accent) {
        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(30, 30); iconBadge.setMinSize(30, 30); iconBadge.setMaxSize(30, 30);
        Region iconBg = new Region(); iconBg.setPrefSize(30, 30);
        iconBg.setStyle("-fx-background-color: " + accent + "20; -fx-background-radius: 8;");
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 14px;");
        iconBadge.getChildren().addAll(iconBg, iconLbl);
        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + DARK + ";"
        );
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Circle accentDot = new Circle(4); accentDot.setFill(Color.web(accent));
        header.getChildren().addAll(iconBadge, titleLbl, sp, accentDot);
        return header;
    }

    private Region divider() {
        Region r = new Region(); r.setPrefHeight(1); r.setMaxHeight(1);
        r.setStyle("-fx-background-color: " + BORDER + ";");
        return r;
    }

    private Label muted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + "; -fx-padding: 8 0 0 0;");
        return l;
    }

    private VBox emptyState(String ico, String text) {
        VBox box = new VBox(6); box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 0, 12, 0));
        Label icoLbl = new Label(ico); icoLbl.setStyle("-fx-font-size: 28px;");
        Label txtLbl = new Label(text); txtLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        box.getChildren().addAll(icoLbl, txtLbl);
        return box;
    }

    private HBox infoRow(String icon, String value, String textColor, String hoverBg) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setStyle("-fx-background-radius: 8;");
        Label ico = new Label(icon); ico.setStyle("-fx-font-size: 13px; -fx-min-width: 22;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + textColor + ";");
        val.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(ico, val);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + hoverBg + "; -fx-background-radius: 8;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-radius: 8;"));
        return row;
    }

    private Label roleBadge(String role) {
        String[] cfg = switch (role) {
            case "ADMIN"     -> new String[]{RED,    RED_L};
            case "MANAGER"   -> new String[]{BLUE,   BLUE_L};
            case "RH"        -> new String[]{PURPLE, PURPLE_L};
            case "FORMATEUR" -> new String[]{GREEN,  GREEN_L};
            case "CANDIDAT"  -> new String[]{ORANGE, ORANGE_L};
            default          -> new String[]{MUTED,  SURFACE};
        };
        Label lbl = new Label(role);
        lbl.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + cfg[0] + ";" +
                        "-fx-background-color: " + cfg[1] + "; -fx-padding: 3 12 3 12; -fx-background-radius: 20;" +
                        "-fx-border-color: " + cfg[0] + "44; -fx-border-radius: 20; -fx-border-width: 1.5;"
        );
        return lbl;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS DB + UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════
    private int queryInt(String sql, Object... params) {
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer v) ps.setInt(i + 1, v);
                else ps.setString(i + 1, params[i].toString());
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    private String[] statusStyle(String statut) {
        if (statut == null) return new String[]{MUTED, BG};
        return switch (statut.toLowerCase()) {
            case "approuvé", "approuve", "justifiée", "justifiee" -> new String[]{GREEN, GREEN_L};
            case "refusé",   "refuse"                             -> new String[]{RED,   RED_L};
            case "en attente"                                     -> new String[]{AMBER, AMBER_L};
            default                                               -> new String[]{MUTED, BG};
        };
    }

    private void animateCount(Label label, int target) {
        if (target <= 0) return;
        Timeline tl = new Timeline();
        int frames = 28;
        for (int i = 0; i <= frames; i++) {
            final int v = (int) Math.round(target * Math.pow((double) i / frames, 0.6));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(28L * i), e -> label.setText(String.valueOf(v))));
        }
        tl.setDelay(Duration.millis(300)); tl.play();
    }

    private String getGreeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Bonjour" : h < 18 ? "Bon après-midi" : "Bonsoir";
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}