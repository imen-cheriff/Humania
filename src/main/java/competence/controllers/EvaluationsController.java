package competence.controllers;

import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class EvaluationsController {

    @FXML private Label            lblActiveCampaigns;
    @FXML private Label            lblPendingReviews;
    @FXML private Label            lblCompletedMonth;
    @FXML private Label            lblAvgRating;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterType;
    @FXML private Button           btnClearFilters;
    @FXML private Button           btnNewCampaign;
    @FXML private VBox             vboxCampaigns;
    @FXML private VBox             vboxRecentEvaluations;

    private Connection      connection;
    private int             currentUserId = -1;
    private List<EvalEntry> allEvals      = new ArrayList<>();

    private static final DateTimeFormatter FMT_SHORT = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter FMT_FULL  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private boolean isAdmin() {
        String r = UserSession.getInstance().getRole();
        return r != null && r.equalsIgnoreCase("ADMIN");
    }

    private boolean isAdminOrRH() {
        String r = UserSession.getInstance().getRole();
        return r != null && (r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("RH"));
    }

    private int resolveCurrentUserId() {
        try {
            String u = UserSession.getInstance().getUser();
            String e = UserSession.getInstance().getEmail();
            if (u != null && !u.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE username=? LIMIT 1");
                ps.setString(1, u); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            if (e != null && !e.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE email=? LIMIT 1");
                ps.setString(1, e); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ignored) {}
        return 1;
    }

    @FXML
    public void initialize() {
        connection    = MyDataBase.getInstance().getCnx();
        currentUserId = resolveCurrentUserId();
        ensureSchema();
        if (btnNewCampaign != null) { btnNewCampaign.setVisible(isAdmin()); btnNewCampaign.setManaged(isAdmin()); }
        setupFilters();
        loadAll();
    }

    private void setupFilters() {
        if (filterStatus != null) {
            filterStatus.getItems().addAll("Tous les statuts","Accessible","Verrouillée","Pas encore ouverte","Expirée","Réussie","Échouée");
            filterStatus.setValue("Tous les statuts");
            filterStatus.setOnAction(e -> applyFilters());
        }
        if (filterType != null) {
            filterType.getItems().addAll("Tous les types","QCM","Pratique","Oral","Projet","Auto-évaluation");
            filterType.setValue("Tous les types");
            filterType.setOnAction(e -> applyFilters());
        }
        if (searchField != null) searchField.textProperty().addListener((o,ov,nv) -> applyFilters());
        if (btnClearFilters != null) btnClearFilters.setOnAction(e -> {
            if (searchField!=null) searchField.clear();
            if (filterStatus!=null) filterStatus.setValue("Tous les statuts");
            if (filterType!=null) filterType.setValue("Tous les types");
            applyFilters();
        });
    }

    // ── Schema ──────────────────────────────────────────────────────────
    private void ensureSchema() {
        addCol("evaluationformation","score_requis","INT DEFAULT 70");
        addCol("evaluationformation","niveau_succes","INT DEFAULT 1");
        addCol("evaluationformation","niveau_echec","INT DEFAULT -1");
        addCol("evaluationformation","competence_id","INT DEFAULT NULL");
        addCol("evaluationformation","dateDebut","DATE DEFAULT NULL");
        addCol("evaluationformation","dateFin","DATE DEFAULT NULL");
        addCol("evaluationformation","niveau_requis","INT DEFAULT 0");
        addCol("evaluationformation","difficulte","VARCHAR(10) DEFAULT 'MOYEN'");
        addCol("evaluationformation","statut_eval","VARCHAR(10) DEFAULT 'ACTIVE'");
        addCol("resultatevaluation","score_pct","INT DEFAULT 0");
        addCol("resultatevaluation","niveau_delta","INT DEFAULT 0");
        addCol("resultatevaluation","employe_id","INT DEFAULT 0");
        try {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS evaluation_question(" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "evaluation_id INT NOT NULL," +
                            "question TEXT NOT NULL," +
                            "option_a VARCHAR(500)," +
                            "option_b VARCHAR(500)," +
                            "option_c VARCHAR(500)," +
                            "option_d VARCHAR(500)," +
                            "bonne_reponse CHAR(1)," +
                            "explication TEXT," +
                            "INDEX(evaluation_id))").execute();
        } catch (SQLException ignored) {}
    }

    private void addCol(String table, String col, String def) {
        try {
            PreparedStatement chk = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?");
            chk.setString(1,table); chk.setString(2,col);
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt(1)==0)
                connection.prepareStatement("ALTER TABLE `"+table+"` ADD COLUMN `"+col+"` "+def).execute();
        } catch (SQLException ignored) {}
    }

    private boolean colExists(String table, String col) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?");
            ps.setString(1,table); ps.setString(2,col);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1)>0;
        } catch (SQLException e) { return false; }
    }

    // ── Load ────────────────────────────────────────────────────────────
    private void loadAll() {
        loadEvaluations();
        loadStatistics();
        renderCards(allEvals);
        buildSidePanel();
    }

    private void loadStatistics() {
        try {
            long accessible = allEvals.stream().filter(e->e.accessState==AccessState.ACCESSIBLE && e.myStatut==MyStatut.NOT_TAKEN).count();
            if (lblActiveCampaigns!=null) lblActiveCampaigns.setText(String.valueOf(accessible));
            if (lblPendingReviews!=null)  lblPendingReviews.setText(String.valueOf(accessible));
            if (lblCompletedMonth!=null)  lblCompletedMonth.setText(String.valueOf(queryInt("SELECT COUNT(*) FROM resultatevaluation WHERE MONTH(datePassage)=MONTH(CURDATE()) AND YEAR(datePassage)=YEAR(CURDATE())")));
            try {
                ResultSet rs = connection.prepareStatement("SELECT ROUND(AVG(score_pct),1) FROM resultatevaluation WHERE employe_id="+currentUserId+" AND score_pct>0").executeQuery();
                if (rs.next() && lblAvgRating!=null) { String v=rs.getString(1); lblAvgRating.setText(v!=null?v+"%":"—"); }
            } catch (Exception ignored) { if (lblAvgRating!=null) lblAvgRating.setText("—"); }
        } catch (Exception e) { System.err.println("stats: "+e.getMessage()); }
    }

    private int queryInt(String sql) {
        try { ResultSet rs=connection.prepareStatement(sql).executeQuery(); return rs.next()?rs.getInt(1):0; }
        catch (SQLException e) { return 0; }
    }

    private void loadEvaluations() {
        allEvals.clear();
        boolean hasEvalDates  = colExists("evaluationformation","dateDebut");
        boolean hasNivRequis  = colExists("evaluationformation","niveau_requis");
        boolean hasScoreRequis= colExists("evaluationformation","score_requis");
        String datePart = hasEvalDates
                ? "COALESCE(ef.dateDebut,sf.dateDebut) AS eval_debut,COALESCE(ef.dateFin,sf.dateFin) AS eval_fin"
                : "sf.dateDebut AS eval_debut,sf.dateFin AS eval_fin";
        String q =
                "SELECT ef.id,ef.titre,ef.type,ef.duree,"+
                        (hasScoreRequis?"IFNULL(ef.score_requis,70)":"70")+" AS score_requis,"+
                        (hasScoreRequis?"IFNULL(ef.niveau_succes,1)":"1")+" AS niv_ok,"+
                        (hasScoreRequis?"IFNULL(ef.niveau_echec,-1)":"-1")+" AS niv_ko,"+
                        (hasScoreRequis?"IFNULL(ef.competence_id,0)":"0")+" AS competence_id,"+
                        (hasNivRequis?"IFNULL(ef.niveau_requis,0)":"0")+" AS niveau_requis,"+
                        (colExists("evaluationformation","difficulte")?"IFNULL(ef.difficulte,'MOYEN')":"'MOYEN'")+" AS difficulte,"+
                        (colExists("evaluationformation","statut_eval")?"IFNULL(ef.statut_eval,'ACTIVE')":"'ACTIVE'")+" AS statut_eval,"+
                        datePart+",f.titre AS formation_titre,"+
                        "(SELECT COUNT(*) FROM inscriptionformation WHERE session_id=sf.id) AS nb_part,"+
                        "(SELECT COUNT(*) FROM resultatevaluation re2 WHERE re2.evaluation_id=ef.id) AS nb_done,"+
                        "IFNULL((SELECT re.statut FROM resultatevaluation re WHERE re.evaluation_id=ef.id AND re.employe_id=? LIMIT 1),'not_taken') AS my_statut,"+
                        "IFNULL((SELECT re.score_pct FROM resultatevaluation re WHERE re.evaluation_id=ef.id AND re.employe_id=? LIMIT 1),0) AS my_score"+
                        " FROM evaluationformation ef"+
                        " JOIN sessionformation sf ON ef.session_id=sf.id"+
                        " JOIN formation f ON sf.formation_id=f.id"+
                        " ORDER BY eval_debut ASC,ef.competence_id,niveau_requis";
        try {
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1,currentUserId); ps.setInt(2,currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate debut = rs.getDate("eval_debut")!=null ? rs.getDate("eval_debut").toLocalDate() : null;
                LocalDate fin   = rs.getDate("eval_fin")!=null   ? rs.getDate("eval_fin").toLocalDate()   : null;
                int competenceId = rs.getInt("competence_id");
                int niveauRequis = rs.getInt("niveau_requis");
                int niveauActuel = getNiveauActuel(competenceId);
                int niveauMax    = getNiveauMax(competenceId);
                boolean levelOk  = (competenceId==0)||(niveauActuel>=niveauRequis);
                DateStatus  ds   = computeDateStatus(debut,fin);
                AccessState access;
                if (!levelOk)                     access=AccessState.LOCKED_LEVEL;
                else if (ds==DateStatus.NOT_YET)  access=AccessState.NOT_YET;
                else if (ds==DateStatus.EXPIRED)  access=AccessState.EXPIRED;
                else                              access=AccessState.ACCESSIBLE;
                MyStatut ms = switch(rs.getString("my_statut")) {
                    case "passed" -> MyStatut.PASSED; case "failed" -> MyStatut.FAILED; default -> MyStatut.NOT_TAKEN;
                };
                allEvals.add(new EvalEntry(
                        rs.getInt("id"),rs.getString("titre"),rs.getString("type"),rs.getString("formation_titre"),
                        debut,fin,rs.getInt("nb_part"),rs.getInt("nb_done"),
                        rs.getInt("score_requis"),rs.getInt("niv_ok"),rs.getInt("niv_ko"),
                        competenceId,niveauRequis,niveauActuel,niveauMax,
                        rs.getInt("duree"),ds,access,ms,rs.getInt("my_score"),
                        rs.getString("difficulte"),rs.getString("statut_eval")));
            }
        } catch (SQLException e) { System.err.println("loadEvaluations: "+e.getMessage()); }
    }

    private DateStatus computeDateStatus(LocalDate debut, LocalDate fin) {
        if (debut==null||fin==null) return DateStatus.OPEN;
        LocalDate today=LocalDate.now();
        if (today.isBefore(debut)) return DateStatus.NOT_YET;
        if (today.isAfter(fin))    return DateStatus.EXPIRED;
        return DateStatus.OPEN;
    }

    private int getNiveauActuel(int competenceId) {
        if (competenceId<=0) return 0;
        try {
            PreparedStatement ps=connection.prepareStatement("SELECT niveauActuel FROM competenceemploye WHERE employe_id=? AND competence_id=?");
            ps.setInt(1,currentUserId); ps.setInt(2,competenceId);
            ResultSet rs=ps.executeQuery(); return rs.next()?rs.getInt(1):0;
        } catch (SQLException e) { return 0; }
    }

    private int getNiveauMax(int competenceId) {
        if (competenceId<=0) return 5;
        try { ResultSet rs=connection.prepareStatement("SELECT niveauMax FROM competence WHERE id="+competenceId).executeQuery(); return rs.next()?rs.getInt(1):5; }
        catch (SQLException e) { return 5; }
    }

    private String getCompetenceName(int id) {
        if (id<=0) return null;
        try { ResultSet rs=connection.prepareStatement("SELECT libelle FROM competence WHERE id="+id).executeQuery(); return rs.next()?rs.getString(1):null; }
        catch (SQLException e) { return null; }
    }

    // ── Filters ─────────────────────────────────────────────────────────
    private void applyFilters() {
        String search  = searchField!=null ? searchField.getText().toLowerCase().trim() : "";
        String statF   = filterStatus!=null ? filterStatus.getValue() : "Tous les statuts";
        String typeF   = filterType!=null   ? filterType.getValue()   : "Tous les types";
        List<EvalEntry> filtered = allEvals.stream().filter(e -> {
            if (!search.isEmpty() && !e.titre.toLowerCase().contains(search) && !e.formationTitre.toLowerCase().contains(search)) return false;
            if (!"Tous les types".equals(typeF) && !typeF.equalsIgnoreCase(e.type)) return false;
            if (!"Tous les statuts".equals(statF)) return switch(statF) {
                case "Accessible"         -> e.accessState==AccessState.ACCESSIBLE;
                case "Verrouillée"        -> e.accessState==AccessState.LOCKED_LEVEL;
                case "Pas encore ouverte" -> e.accessState==AccessState.NOT_YET;
                case "Expirée"            -> e.accessState==AccessState.EXPIRED;
                case "Réussie"            -> e.myStatut==MyStatut.PASSED;
                case "Échouée"            -> e.myStatut==MyStatut.FAILED;
                default -> true;
            };
            return true;
        }).collect(Collectors.toList());
        renderCards(filtered);
    }

    // ── Cards ────────────────────────────────────────────────────────────
    private void renderCards(List<EvalEntry> list) {
        if (vboxCampaigns==null) return;
        vboxCampaigns.getChildren().clear();
        if (list.isEmpty()) { vboxCampaigns.getChildren().add(buildEmptyState()); return; }
        for (EvalEntry e : list) vboxCampaigns.getChildren().add(buildCard(e));
    }

    private VBox buildCard(EvalEntry e) {
        String[] theme = getTheme(e);
        String accent=theme[0], accentBg=theme[1];
        VBox card = new VBox(0); card.setMaxWidth(Double.MAX_VALUE);
        String baseStyle = "-fx-background-color:white;-fx-background-radius:16;-fx-border-color:"+accentBg+";-fx-border-width:1;-fx-border-radius:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);";
        card.setStyle(baseStyle);
        Region strip = new Region(); strip.setPrefHeight(4); strip.setStyle("-fx-background-color:"+accent+";-fx-background-radius:16 16 0 0;");
        HBox main = new HBox(14); main.setPadding(new Insets(16,20,12,18)); main.setAlignment(Pos.CENTER_LEFT);
        StackPane iconCircle=new StackPane(); iconCircle.setPrefSize(46,46); iconCircle.setMinSize(46,46);
        iconCircle.getChildren().addAll(new Circle(23,Color.web(accentBg)),makeIconLabel(getTypeIcon(e.type)));
        VBox info = new VBox(4); HBox.setHgrow(info,Priority.ALWAYS);
        HBox titleRow=new HBox(8); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl=new Label(e.titre!=null&&!e.titre.isBlank()?e.titre:e.formationTitre);
        titleLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        titleRow.getChildren().addAll(titleLbl,buildStatusBadge(e));
        info.getChildren().add(titleRow);
        HBox subRow=new HBox(8); subRow.setAlignment(Pos.CENTER_LEFT);
        if (e.type!=null) { Label tl=new Label(e.type); tl.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;-fx-background-color:#F1F5F9;-fx-padding:2 7;-fx-background-radius:6;"); subRow.getChildren().add(tl); }
        Label fl=new Label("📚 "+e.formationTitre); fl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;"); subRow.getChildren().add(fl);
        info.getChildren().add(subRow);
        if (e.competenceId>0&&e.niveauMax>0) info.getChildren().add(buildLevelBar(e));
        info.getChildren().add(buildDateRow(e));
        if (e.myStatut!=MyStatut.NOT_TAKEN&&e.myScore>0) info.getChildren().add(buildScoreRow(e));
        VBox right=new VBox(5); right.setAlignment(Pos.CENTER_RIGHT); right.setMinWidth(110);
        int pctDone=e.nbParticipants>0?e.nbDone*100/e.nbParticipants:0;
        right.getChildren().addAll(buildMiniDonut(e.nbParticipants>0?(double)e.nbDone/e.nbParticipants:0,accent),makeSmallLabel("👥 "+e.nbDone+"/"+e.nbParticipants));
        if (isAdmin()&&e.dateDebut==null) { Button bd=new Button("📅 Dates"); bd.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;-fx-font-size:9px;-fx-cursor:hand;-fx-background-radius:6;-fx-padding:3 8;"); bd.setOnAction(ev->openSetDatesDialog(e)); right.getChildren().add(bd); }
        main.getChildren().addAll(iconCircle,info,right);
        HBox barRow=buildProgressBar(pctDone,accent);
        HBox footer=buildFooter(e,accent);
        card.getChildren().addAll(strip,main,barRow,footer);
        String hover="-fx-background-color:white;-fx-background-radius:16;-fx-border-color:"+accent+";-fx-border-width:1.5;-fx-border-radius:16;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),18,0,0,4);";
        card.setOnMouseEntered(ev->card.setStyle(hover)); card.setOnMouseExited(ev->card.setStyle(baseStyle));
        return card;
    }

    private Label makeIconLabel(String ico) { Label l=new Label(ico); l.setStyle("-fx-font-size:17px;"); return l; }
    private Label makeSmallLabel(String t)  { Label l=new Label(t);   l.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;"); return l; }

    private Label buildStatusBadge(EvalEntry e) {
        if (e.myStatut==MyStatut.PASSED) return makePill("✓ Réussi","#059669","#ECFDF5");
        if (e.myStatut==MyStatut.FAILED) return makePill("✗ Échoué","#DC2626","#FEF2F2");
        return switch(e.accessState) {
            case ACCESSIBLE   -> makePill("🔓 Accessible","#4F46E5","#EEF2FF");
            case LOCKED_LEVEL -> makePill("🔒 Verrouillée","#92400E","#FEF3C7");
            case NOT_YET      -> makePill("⏳ Bientôt","#D97706","#FEF3C7");
            case EXPIRED      -> makePill("🔒 Expirée","#6B7280","#F3F4F6");
        };
    }

    private HBox buildLevelBar(EvalEntry e) {
        HBox row=new HBox(6); row.setAlignment(Pos.CENTER_LEFT);
        String compName=getCompetenceName(e.competenceId);
        Label compLbl=new Label(compName!=null?compName:"Compétence");
        compLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;-fx-font-weight:bold;");
        HBox dots=new HBox(3); dots.setAlignment(Pos.CENTER_LEFT);
        for (int i=1;i<=e.niveauMax;i++) {
            StackPane dot=new StackPane(); dot.setPrefSize(16,16);
            Circle c=new Circle(8);
            if (i<=e.niveauActuel) { c.setFill(Color.web("#6366F1")); }
            else if (i==e.niveauRequis+1&&e.niveauActuel<e.niveauRequis) { c.setFill(Color.web("#FEF3C7")); c.setStroke(Color.web("#F59E0B")); c.setStrokeWidth(2); }
            else if (i<=e.niveauRequis) { c.setFill(Color.web("#FEF2F2")); c.setStroke(Color.web("#EF4444")); c.setStrokeWidth(1.5); }
            else { c.setFill(Color.web("#F1F5F9")); c.setStroke(Color.web("#D1D5DB")); c.setStrokeWidth(1); }
            Label num=new Label(String.valueOf(i));
            num.setStyle("-fx-font-size:7px;-fx-font-weight:bold;-fx-text-fill:"+(i<=e.niveauActuel?"white":(i<=e.niveauRequis?"#EF4444":"#9CA3AF"))+";");
            dot.getChildren().addAll(c,num); dots.getChildren().add(dot);
        }
        Label lvInfo=new Label(e.niveauActuel+"/"+e.niveauMax); lvInfo.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;");
        row.getChildren().addAll(compLbl,dots,lvInfo);
        if (e.accessState==AccessState.LOCKED_LEVEL) {
            Label lockInfo=new Label("→ Requis : niv."+e.niveauRequis);
            lockInfo.setStyle("-fx-font-size:9px;-fx-text-fill:#D97706;-fx-background-color:#FEF3C7;-fx-padding:1 6;-fx-background-radius:5;");
            row.getChildren().add(lockInfo);
        }
        String okSign=e.niveauSucces>0?"+":"";
        Label ok=new Label("✓ "+okSign+e.niveauSucces); ok.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#059669;-fx-background-color:#ECFDF5;-fx-padding:1 5;-fx-background-radius:5;");
        String koSign=e.niveauEchec<=0?"":"+";
        Label ko=new Label("✗ "+koSign+e.niveauEchec); ko.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#DC2626;-fx-background-color:#FEF2F2;-fx-padding:1 5;-fx-background-radius:5;");
        row.getChildren().addAll(ok,ko);
        return row;
    }

    private HBox buildDateRow(EvalEntry e) {
        HBox row=new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        if (e.dateDebut!=null&&e.dateFin!=null) {
            Label dl=new Label("📅 "+e.dateDebut.format(FMT_SHORT)+" → "+e.dateFin.format(FMT_SHORT));
            dl.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;"); row.getChildren().add(dl);
            LocalDate today=LocalDate.now();
            if (e.dateStatus==DateStatus.OPEN) {
                long d=ChronoUnit.DAYS.between(today,e.dateFin);
                String txt=d==0?"⏰ Dernier jour !":d==1?"⏰ 1j restant":"⏰ "+d+"j restants";
                String col=d<=2?"#DC2626":d<=7?"#D97706":"#059669";
                Label cd=new Label(txt); cd.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:"+col+";");
                row.getChildren().add(cd);
            } else if (e.dateStatus==DateStatus.NOT_YET) {
                Label cd=new Label("Dans "+ChronoUnit.DAYS.between(today,e.dateDebut)+"j"); cd.setStyle("-fx-font-size:10px;-fx-text-fill:#D97706;"); row.getChildren().add(cd);
            } else {
                Label cd=new Label("Fermée depuis "+ChronoUnit.DAYS.between(e.dateFin,today)+"j"); cd.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;"); row.getChildren().add(cd);
            }
        } else {
            Label nd=new Label("📅 Dates non définies"); nd.setStyle("-fx-font-size:10px;-fx-text-fill:#D97706;"); row.getChildren().add(nd);
            if (isAdmin()) { Button b=new Button("Définir"); b.setStyle("-fx-background-color:transparent;-fx-text-fill:#4F46E5;-fx-font-size:9px;-fx-cursor:hand;-fx-border-color:#C7D2FE;-fx-border-radius:5;-fx-background-radius:5;-fx-padding:2 8;"); b.setOnAction(ev->openSetDatesDialog(e)); row.getChildren().add(b); }
        }
        return row;
    }

    private HBox buildScoreRow(EvalEntry e) {
        HBox row=new HBox(7); row.setAlignment(Pos.CENTER_LEFT);
        boolean passed=e.myStatut==MyStatut.PASSED; String color=passed?"#10B981":"#EF4444";
        Label s=new Label("Score : "+e.myScore+"%"); s.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        StackPane bar=new StackPane(); bar.setPrefSize(80,5);
        Region tr=new Region(); tr.setPrefSize(80,5); tr.setStyle("-fx-background-color:#F1F5F9;-fx-background-radius:3;");
        Region fi=new Region(); fi.setPrefSize(Math.min(80,e.myScore*0.8),5); fi.setStyle("-fx-background-color:"+color+";-fx-background-radius:3;");
        StackPane.setAlignment(fi,Pos.CENTER_LEFT); bar.getChildren().addAll(tr,fi);
        int delta=passed?e.niveauSucces:e.niveauEchec;
        Label d=new Label((delta>0?"+":"")+delta+" niv."); d.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:"+(delta>0?"#059669":"#DC2626")+";-fx-background-color:"+(delta>0?"#ECFDF5":"#FEF2F2")+";-fx-padding:1 6;-fx-background-radius:8;");
        row.getChildren().addAll(s,bar,d);
        return row;
    }

    private HBox buildProgressBar(int pct, String accent) {
        HBox row=new HBox(); row.setPadding(new Insets(0,18,0,18));
        StackPane wrap=new StackPane(); wrap.setPrefHeight(5); HBox.setHgrow(wrap,Priority.ALWAYS);
        Region tr=new Region(); tr.setPrefHeight(5); tr.setStyle("-fx-background-color:#F1F5F9;-fx-background-radius:3;"); tr.prefWidthProperty().bind(wrap.widthProperty());
        Region fi=new Region(); fi.setPrefHeight(5); fi.setStyle("-fx-background-color:"+accent+";-fx-background-radius:3;"); StackPane.setAlignment(fi,Pos.CENTER_LEFT);
        wrap.widthProperty().addListener((obs,o,nw)->fi.setPrefWidth(nw.doubleValue()*pct/100.0));
        wrap.getChildren().addAll(tr,fi); row.getChildren().add(wrap);
        return row;
    }

    private StackPane buildMiniDonut(double ratio, String color) {
        Canvas c=new Canvas(48,48); GraphicsContext gc=c.getGraphicsContext2D();
        gc.setStroke(Color.web("#F1F5F9")); gc.setLineWidth(5); gc.strokeOval(7,7,34,34);
        if (ratio>0) { gc.setStroke(Color.web(color)); gc.setLineCap(StrokeLineCap.ROUND); gc.strokeArc(7,7,34,34,90,-(ratio*360),javafx.scene.shape.ArcType.OPEN); }
        Label l=new Label(String.format("%.0f%%",ratio*100)); l.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        StackPane sp=new StackPane(c,l); sp.setPrefSize(48,48); return sp;
    }

    private HBox buildFooter(EvalEntry e, String accent) {
        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 18, 14, 18));
        footer.setAlignment(Pos.CENTER_LEFT);

        // Meta infos
        Label dl = new Label("⏱ " + e.duree + " min");
        dl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
        Label sl = new Label("🎯 " + e.scoreRequis + "%");
        sl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(dl, sl, sp);

        // ── Bouton principal (passer / statut) ────────────────────────────
        Button mainBtn = new Button(); mainBtn.setPrefHeight(34);
        if (e.myStatut == MyStatut.PASSED) {
            mainBtn.setText("🏆  Réussie !"); mainBtn.setDisable(true);
            mainBtn.setStyle("-fx-background-color:#ECFDF5;-fx-text-fill:#059669;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:7 16;-fx-background-radius:18;-fx-border-color:#6EE7B7;-fx-border-width:1;-fx-border-radius:18;");
        } else if (e.accessState == AccessState.LOCKED_LEVEL) {
            String nm = getCompetenceName(e.competenceId);
            String nmS = nm != null ? (nm.length() > 10 ? nm.substring(0, 9) + "…" : nm) : "";
            mainBtn.setText("🔒  Niv." + e.niveauRequis + " requis" + (nmS.isEmpty() ? "" : " · " + nmS));
            mainBtn.setDisable(true);
            mainBtn.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;-fx-font-size:10px;-fx-padding:7 14;-fx-background-radius:18;-fx-border-color:#FCD34D;-fx-border-width:1;-fx-border-radius:18;");
        } else if (e.accessState == AccessState.NOT_YET) {
            mainBtn.setText("⏳  Dispo. le " + (e.dateDebut != null ? e.dateDebut.format(FMT_FULL) : "?"));
            mainBtn.setDisable(true);
            mainBtn.setStyle("-fx-background-color:#F3F4F6;-fx-text-fill:#6B7280;-fx-font-size:10px;-fx-padding:7 14;-fx-background-radius:18;-fx-border-color:#D1D5DB;-fx-border-width:1;-fx-border-radius:18;");
        } else if (e.accessState == AccessState.EXPIRED) {
            mainBtn.setText("🔒  Expirée");
            mainBtn.setDisable(true);
            mainBtn.setStyle("-fx-background-color:#F9FAFB;-fx-text-fill:#9CA3AF;-fx-font-size:10px;-fx-padding:7 14;-fx-background-radius:18;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:18;");
        } else if (e.myStatut == MyStatut.FAILED) {
            mainBtn.setText("↺  Repasser");
            styleActiveBtn(mainBtn, "#F59E0B");
            mainBtn.setOnAction(ev -> openQuiz(e));
        } else {
            mainBtn.setText("📝  Passer l'évaluation");
            styleActiveBtn(mainBtn, accent);
            mainBtn.setOnAction(ev -> openQuiz(e));
        }
        footer.getChildren().add(mainBtn);

        // ── Bouton Détails (toujours visible) ────────────────────────────
        Button btnDetails = new Button("👁  Détails");
        btnDetails.setStyle("-fx-background-color:transparent;-fx-text-fill:#6366F1;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-border-color:#C7D2FE;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;");
        btnDetails.setOnMouseEntered(ev -> btnDetails.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-border-color:#818CF8;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;"));
        btnDetails.setOnMouseExited(ev -> btnDetails.setStyle("-fx-background-color:transparent;-fx-text-fill:#6366F1;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-border-color:#C7D2FE;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;"));
        btnDetails.setOnAction(ev -> openDetailDialog(e));
        footer.getChildren().add(btnDetails);

        // ── Boutons Admin : Modifier + Désactiver/Supprimer ─────────────
        if (isAdmin()) {
            Button btnEdit = new Button("✏  Modifier");
            btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:#D97706;-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-cursor:hand;-fx-border-color:#FCD34D;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;");
            btnEdit.setOnMouseEntered(ev -> btnEdit.setStyle("-fx-background-color:#FFFBEB;-fx-text-fill:#92400E;-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-cursor:hand;-fx-border-color:#F59E0B;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;"));
            btnEdit.setOnMouseExited(ev -> btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:#D97706;-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-cursor:hand;-fx-border-color:#FCD34D;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;"));
            btnEdit.setOnAction(ev -> openEditDialog(e));
            footer.getChildren().add(btnEdit);

            // Désactiver / Réactiver
            boolean isActive = !"INACTIVE".equals(e.statutEval);
            Button btnToggle = new Button(isActive ? "⊘  Désactiver" : "✓  Réactiver");
            String tBase = isActive
                    ? "-fx-background-color:transparent;-fx-text-fill:#9CA3AF;-fx-font-size:11px;-fx-font-weight:bold;-fx-cursor:hand;-fx-border-color:#E5E7EB;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;"
                    : "-fx-background-color:transparent;-fx-text-fill:#059669;-fx-font-size:11px;-fx-font-weight:bold;-fx-cursor:hand;-fx-border-color:#BBF7D0;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:7 14;";
            btnToggle.setStyle(tBase);
            btnToggle.setOnAction(ev -> confirmToggleStatus(e, isActive));
            footer.getChildren().add(btnToggle);

            // Supprimer
            Button btnDel = new Button("🗑");
            btnDel.setStyle("-fx-background-color:transparent;-fx-text-fill:#EF4444;-fx-font-size:13px;-fx-cursor:hand;" +
                    "-fx-border-color:transparent;-fx-background-radius:8;-fx-padding:4 8;");
            btnDel.setOnMouseEntered(ev -> btnDel.setStyle("-fx-background-color:#FEF2F2;-fx-text-fill:#DC2626;-fx-font-size:13px;-fx-cursor:hand;" +
                    "-fx-border-color:transparent;-fx-background-radius:8;-fx-padding:4 8;"));
            btnDel.setOnMouseExited(ev -> btnDel.setStyle("-fx-background-color:transparent;-fx-text-fill:#EF4444;-fx-font-size:13px;-fx-cursor:hand;" +
                    "-fx-border-color:transparent;-fx-background-radius:8;-fx-padding:4 8;"));
            btnDel.setOnAction(ev -> confirmDelete(e));
            footer.getChildren().add(btnDel);
        }

        return footer;
    }

    private void styleActiveBtn(Button btn, String color) {
        String base="-fx-background-color:"+color+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:8 20;-fx-background-radius:20;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,"+color+"55,8,0,0,2);";
        btn.setStyle(base);
        btn.setOnMouseEntered(ev->btn.setStyle(base.replace(color,"derive("+color+",-10%)")));
        btn.setOnMouseExited(ev->btn.setStyle(base));
    }

    private VBox buildEmptyState() {
        VBox box=new VBox(10); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(40));
        Label ico=new Label("📋"); ico.setStyle("-fx-font-size:34px;");
        Label msg=new Label("Aucune évaluation ne correspond à vos critères"); msg.setStyle("-fx-font-size:13px;-fx-text-fill:#9CA3AF;");
        box.getChildren().addAll(ico,msg); return box;
    }

    // ── Panel droit ──────────────────────────────────────────────────────
    private void buildSidePanel() {
        if (vboxRecentEvaluations==null) return;
        vboxRecentEvaluations.getChildren().clear();
        vboxRecentEvaluations.getChildren().add(buildTimelinePanel());
        vboxRecentEvaluations.getChildren().add(buildHistoryPanel());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CALENDRIER DYNAMIQUE — navigation mois par mois
    // ══════════════════════════════════════════════════════════════════════
    private LocalDate calendarMonth = LocalDate.now().withDayOfMonth(1);
    private VBox      calendarContainer = null; // référence pour refresh

    public VBox buildTimelinePanel() {
        VBox panel = new VBox(0);
        panel.setStyle(
                "-fx-background-color:white;-fx-background-radius:16;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:16;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),10,0,0,2);"
        );

        LocalDate today = LocalDate.now();
        calendarMonth = today.withDayOfMonth(1); // reset à aujourd'hui à chaque refresh

        // ── Header navigation ──────────────────────────────────────────────
        HBox hdr = new HBox(8);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(12, 14, 10, 14));
        hdr.setStyle("-fx-border-color:transparent transparent #F1F5F9 transparent;-fx-border-width:0 0 1 0;");

        Button btnPrev = makeCalNavBtn("‹");
        Button btnNext = makeCalNavBtn("›");
        Button btnToday = new Button("Aujourd'hui");
        btnToday.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;-fx-font-size:9px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:8;-fx-padding:4 10;-fx-border-color:transparent;");

        Label monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        hdr.getChildren().addAll(new Label("📅"), monthLabel, sp1, btnToday, btnPrev, btnNext);
        panel.getChildren().add(hdr);

        // ── Contenu calendrier (VBox réfreshable) ─────────────────────────
        calendarContainer = new VBox(0);
        panel.getChildren().add(calendarContainer);

        // ── Légende ───────────────────────────────────────────────────────
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 14, 10, 14));
        legend.setStyle("-fx-border-color:#F1F5F9 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        String[][] leg = {
                {"#6366F1","Accessible"}, {"#F59E0B","Bientôt"},
                {"#10B981","Réussie"}, {"#EF4444","Échouée"},
                {"#9CA3AF","Expirée"}, {"#DC2626","●","DIFFICILE"},
                {"#6366F1","●","MOYEN"}, {"#10B981","●","FACILE"}
        };
        for (String[] l : leg) {
            if (l.length == 3) continue; // difficultés dans la légende diff
            HBox item = new HBox(3); item.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(4, Color.web(l[0]));
            Label lb = new Label(l[1]); lb.setStyle("-fx-font-size:8px;-fx-text-fill:#6B7280;");
            item.getChildren().addAll(dot, lb);
            legend.getChildren().add(item);
        }
        // Séparateur difficulté
        Label diffTitle = new Label("  Difficulté :");
        diffTitle.setStyle("-fx-font-size:8px;-fx-text-fill:#9CA3AF;");
        legend.getChildren().add(diffTitle);
        for (String[] diff : new String[][]{{"🟢","FACILE"},{"🔵","MOYEN"},{"🔴","DIFFICILE"}}) {
            Label dl = new Label(diff[0] + " " + diff[1]);
            dl.setStyle("-fx-font-size:8px;-fx-text-fill:#6B7280;");
            legend.getChildren().add(dl);
        }
        panel.getChildren().add(legend);

        // Action navigation
        Runnable refresh = () -> {
            String mName = calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            mName = mName.substring(0,1).toUpperCase() + mName.substring(1);
            monthLabel.setText(mName);
            calendarContainer.getChildren().clear();
            calendarContainer.getChildren().add(buildMonthBlock(calendarMonth, today));
        };
        btnPrev.setOnAction(ev -> { calendarMonth = calendarMonth.minusMonths(1); refresh.run(); });
        btnNext.setOnAction(ev -> { calendarMonth = calendarMonth.plusMonths(1); refresh.run(); });
        btnToday.setOnAction(ev -> { calendarMonth = today.withDayOfMonth(1); refresh.run(); });
        refresh.run(); // init

        return panel;
    }

    private Button makeCalNavBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:8;-fx-padding:3 10;-fx-border-color:transparent;");
        btn.setOnMouseEntered(ev -> btn.setStyle("-fx-background-color:#E2E8F6;-fx-text-fill:#111827;-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:8;-fx-padding:3 10;-fx-border-color:transparent;"));
        btn.setOnMouseExited(ev -> btn.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:8;-fx-padding:3 10;-fx-border-color:transparent;"));
        return btn;
    }

    private VBox buildMonthBlock(LocalDate month, LocalDate today) {
        VBox block = new VBox(6);
        block.setPadding(new Insets(10, 14, 6, 14));
        boolean isCurrent = month.getMonthValue() == today.getMonthValue()
                && month.getYear() == today.getYear();

        // Mois header
        String monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        HBox monthHdr = new HBox(6);
        monthHdr.setAlignment(Pos.CENTER_LEFT);
        Label ml = new Label(monthName);
        ml.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" +
                (isCurrent ? "#6366F1" : "#9CA3AF") + ";");
        if (isCurrent) {
            Label curTag = new Label("Maintenant");
            curTag.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;" +
                    "-fx-font-size:8px;-fx-font-weight:bold;-fx-padding:2 7;-fx-background-radius:6;");
            monthHdr.getChildren().addAll(ml, curTag);
        } else {
            monthHdr.getChildren().add(ml);
        }
        block.getChildren().add(monthHdr);

        // Grille semaines (7 colonnes)
        GridPane grid = new GridPane();
        grid.setHgap(3); grid.setVgap(3);

        // Jours de la semaine
        String[] days = {"L","M","M","J","V","S","D"};
        for (int d = 0; d < 7; d++) {
            Label dl = new Label(days[d]);
            dl.setPrefWidth(26); dl.setPrefHeight(16);
            dl.setAlignment(Pos.CENTER);
            dl.setStyle("-fx-font-size:8px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
            grid.add(dl, d, 0);
        }

        LocalDate firstDay = month.withDayOfMonth(1);
        int firstDow = firstDay.getDayOfWeek().getValue() - 1; // 0=lundi
        int daysInMonth = month.lengthOfMonth();

        // Collecter les évals de ce mois
        LocalDate mStart = month.withDayOfMonth(1);
        LocalDate mEnd   = month.withDayOfMonth(daysInMonth);
        Map<Integer, List<EvalEntry>> evalsByDay = new HashMap<>();
        for (EvalEntry e : allEvals) {
            if (e.dateDebut == null || e.dateFin == null) continue;
            if (e.dateFin.isBefore(mStart) || e.dateDebut.isAfter(mEnd)) continue;
            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate day = month.withDayOfMonth(d);
                if (!day.isBefore(e.dateDebut) && !day.isAfter(e.dateFin)) {
                    evalsByDay.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
                }
            }
        }

        for (int day = 1; day <= daysInMonth; day++) {
            int col = (firstDow + day - 1) % 7;
            int row = (firstDow + day - 1) / 7 + 1;
            LocalDate curDay = month.withDayOfMonth(day);
            List<EvalEntry> dayEvals = evalsByDay.getOrDefault(day, Collections.emptyList());
            boolean isToday = curDay.equals(today);

            StackPane cell = buildDayCell(day, isToday, dayEvals);
            grid.add(cell, col, row);
        }

        block.getChildren().add(grid);

        // Évals de ce mois (liste compacte)
        List<EvalEntry> monthEvals = allEvals.stream()
                .filter(e -> e.dateDebut != null && e.dateFin != null)
                .filter(e -> !e.dateFin.isBefore(mStart) && !e.dateDebut.isAfter(mEnd))
                .distinct()
                .collect(Collectors.toList());

        if (!monthEvals.isEmpty()) {
            VBox evList = new VBox(3);
            evList.setPadding(new Insets(6, 0, 0, 0));
            for (EvalEntry e : monthEvals) {
                evList.getChildren().add(buildCalendarEventRow(e));
            }
            block.getChildren().add(evList);
        }

        // Séparateur entre mois
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        block.getChildren().add(sep);
        return block;
    }

    private StackPane buildDayCell(int day, boolean isToday, List<EvalEntry> evals) {
        StackPane cell = new StackPane();
        cell.setPrefSize(26, 26); cell.setMinSize(26, 26); cell.setMaxSize(26, 26);

        // Background
        String bg, fg;
        if (isToday) {
            bg = "-fx-background-color:#6366F1;-fx-background-radius:8;";
            fg = "white";
        } else if (!evals.isEmpty()) {
            String col = getTimelineColor(evals.get(0));
            bg = "-fx-background-color:" + col + "22;-fx-background-radius:8;";
            fg = col;
        } else {
            bg = "-fx-background-color:transparent;";
            fg = "#374151";
        }
        cell.setStyle(bg);

        Label dayLbl = new Label(String.valueOf(day));
        dayLbl.setStyle("-fx-font-size:9px;-fx-font-weight:" +
                (isToday || !evals.isEmpty() ? "bold" : "normal") +
                ";-fx-text-fill:" + fg + ";");
        cell.getChildren().add(dayLbl);

        // Dot indicateur si plusieurs évals
        if (evals.size() > 1) {
            Circle dot = new Circle(2.5, Color.web(getTimelineColor(evals.get(0))));
            StackPane.setAlignment(dot, Pos.BOTTOM_CENTER);
            StackPane.setMargin(dot, new Insets(0, 0, 2, 0));
            cell.getChildren().add(dot);
        }

        return cell;
    }

    private HBox buildCalendarEventRow(EvalEntry e) {
        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 6));
        row.setStyle("-fx-background-radius:8;-fx-cursor:hand;");

        String color = getTimelineColor(e);

        // Accent strip
        Region accent = new Region();
        accent.setPrefSize(3, 26); accent.setMinSize(3, 26);
        accent.setStyle("-fx-background-color:" + color + ";-fx-background-radius:2;");

        // Info
        VBox info = new VBox(1); HBox.setHgrow(info, Priority.ALWAYS);
        String name = e.titre != null && !e.titre.isBlank() ? e.titre : e.formationTitre;
        if (name != null && name.length() > 22) name = name.substring(0, 20) + "…";
        HBox nameRow = new HBox(5); nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        nameRow.getChildren().add(nameLbl);
        // Badge difficulté compact
        Label diffIco = new Label(getDiffIcon(e.difficulte));
        diffIco.setStyle("-fx-font-size:9px;");
        nameRow.getChildren().add(diffIco);

        String dateRange = (e.dateDebut != null ? e.dateDebut.format(DateTimeFormatter.ofPattern("dd/MM")) : "?") +
                " → " + (e.dateFin != null ? e.dateFin.format(DateTimeFormatter.ofPattern("dd/MM")) : "?");
        Label dateLbl = new Label(dateRange + "  " + getDiffLabel(e.difficulte));
        dateLbl.setStyle("-fx-font-size:8px;-fx-text-fill:#9CA3AF;");
        info.getChildren().addAll(nameRow, dateLbl);

        // Status pill
        String pillText = switch (e.accessState) {
            case ACCESSIBLE   -> e.myStatut == MyStatut.PASSED ? "✓" :
                    e.myStatut == MyStatut.FAILED ? "✗" : "▶";
            case LOCKED_LEVEL -> "🔒";
            case NOT_YET      -> "⏳";
            case EXPIRED      -> "—";
        };
        Label pill = new Label(pillText);
        pill.setStyle("-fx-font-size:10px;-fx-text-fill:" + color + ";" +
                "-fx-background-color:" + color + "18;-fx-padding:2 7;-fx-background-radius:8;");

        row.getChildren().addAll(accent, info, pill);
        row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color:" + color + "10;-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseExited(ev -> row.setStyle("-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseClicked(ev -> openDetailDialog(e));
        return row;
    }

    // ── Difficulté helpers ────────────────────────────────────────────────
    private String getDiffIcon(String diff) {
        if (diff == null) return "🔵";
        return switch (diff.toUpperCase()) {
            case "FACILE"    -> "🟢";
            case "DIFFICILE" -> "🔴";
            default          -> "🔵";
        };
    }

    private String getDiffHint(String diff) {
        return switch (diff != null ? diff.toUpperCase() : "MOYEN") {
            case "FACILE"    -> "🟢 Facile : +1 niv. si réussi · -1 si échoué · Score 60%";
            case "DIFFICILE" -> "🔴 Difficile : +3 niv. si réussi · -2 si échoué · Score 80%";
            default          -> "🔵 Moyen : +2 niv. si réussi · -1 si échoué · Score 70%";
        };
    }

    private String getDiffLabel(String diff) {
        if (diff == null) return "Moyen";
        return switch (diff.toUpperCase()) {
            case "FACILE"    -> "Facile";
            case "DIFFICILE" -> "Difficile";
            default          -> "Moyen";
        };
    }

    private String getDiffColor(String diff) {
        if (diff == null) return "#6366F1";
        return switch (diff.toUpperCase()) {
            case "FACILE"    -> "#10B981";
            case "DIFFICILE" -> "#DC2626";
            default          -> "#6366F1";
        };
    }

    private Label buildDiffBadge(String diff) {
        String color = getDiffColor(diff);
        String label = getDiffIcon(diff) + " " + getDiffLabel(diff);
        String bg    = switch ((diff != null ? diff.toUpperCase() : "MOYEN")) {
            case "FACILE"    -> "#ECFDF5";
            case "DIFFICILE" -> "#FEF2F2";
            default          -> "#EEF2FF";
        };
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + color + ";" +
                "-fx-background-color:" + bg + ";-fx-padding:2 8;-fx-background-radius:8;");
        return lbl;
    }

    // ── Confirmer désactivation ────────────────────────────────────────────
    private void confirmToggleStatus(EvalEntry e, boolean currentlyActive) {
        String title   = currentlyActive ? "Désactiver l'évaluation" : "Réactiver l'évaluation";
        String evalName = e.titre != null && !e.titre.isBlank() ? e.titre : e.formationTitre;
        String newStatut = currentlyActive ? "INACTIVE" : "ACTIVE";

        // Dialog personnalisé
        Stage popup = new Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.initOwner(vboxCampaigns.getScene().getWindow());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,5);");

        // Header coloré
        HBox hdr = new HBox(10);
        hdr.setPadding(new Insets(18, 20, 14, 20));
        hdr.setAlignment(Pos.CENTER_LEFT);
        String hdrColor = currentlyActive ? "#FEF2F2" : "#ECFDF5";
        hdr.setStyle("-fx-background-color:" + hdrColor + ";-fx-background-radius:14 14 0 0;");
        Label icoLbl = new Label(currentlyActive ? "⊘" : "✓");
        icoLbl.setStyle("-fx-font-size:22px;-fx-text-fill:" + (currentlyActive ? "#DC2626" : "#059669") + ";");
        VBox hdrInfo = new VBox(3);
        Label hdrTitle = new Label(title);
        hdrTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label hdrName = new Label(evalName);
        hdrName.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        hdrInfo.getChildren().addAll(hdrTitle, hdrName);
        hdr.getChildren().addAll(icoLbl, hdrInfo);
        root.getChildren().add(hdr);

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 20, 6, 20));
        Label msg = new Label(currentlyActive
                ? "Les employés ne pourront plus accéder à cette évaluation. Les résultats existants sont conservés."
            : "L'évaluation sera de nouveau visible et accessible aux employés.");
        msg.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        msg.setWrapText(true);
        body.getChildren().add(msg);
        root.getChildren().add(body);

        // Boutons
        HBox btns = new HBox(10);
        btns.setPadding(new Insets(14, 20, 18, 20));
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:12px;" +
                "-fx-cursor:hand;-fx-background-radius:20;-fx-border-color:transparent;-fx-padding:8 18;");
        btnCancel.setOnAction(ev -> popup.close());
        Button btnConfirm = new Button(currentlyActive ? "Désactiver" : "Réactiver");
        btnConfirm.setStyle("-fx-background-color:" + (currentlyActive ? "#EF4444" : "#10B981") +
                ";-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:20;-fx-border-color:transparent;-fx-padding:8 18;");
        btnConfirm.setOnAction(ev -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE evaluationformation SET statut_eval=? WHERE id=?");
                ps.setString(1, newStatut); ps.setInt(2, e.id);
                ps.executeUpdate();
                popup.close();
                loadAll();
            } catch (SQLException ex) { showError("Erreur", ex.getMessage()); }
        });
        btns.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().add(btns);

        popup.setScene(new javafx.scene.Scene(root, 380, 220));
        popup.showAndWait();
    }

    // ── Confirmer suppression ─────────────────────────────────────────────
    private void confirmDelete(EvalEntry e) {
        String evalName = e.titre != null && !e.titre.isBlank() ? e.titre : e.formationTitre;

        Stage popup = new Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.initOwner(vboxCampaigns.getScene().getWindow());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#FCA5A5;-fx-border-radius:14;-fx-border-width:1.5;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,5);");

        HBox hdr = new HBox(10);
        hdr.setPadding(new Insets(18, 20, 14, 20));
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color:#FEF2F2;-fx-background-radius:14 14 0 0;");
        Label icoLbl = new Label("🗑");
        icoLbl.setStyle("-fx-font-size:22px;");
        VBox hdrInfo = new VBox(3);
        Label hdrTitle = new Label("Supprimer l'évaluation");
        hdrTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#DC2626;");
        Label hdrName = new Label(evalName);
        hdrName.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        hdrInfo.getChildren().addAll(hdrTitle, hdrName);
        hdr.getChildren().addAll(icoLbl, hdrInfo);
        root.getChildren().add(hdr);

        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 20, 4, 20));
        Label warn = new Label("⚠ Cette action est irréversible !");
        warn.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#DC2626;");
        Label msg = new Label("Les questions et résultats associés seront également supprimés. Cette action ne peut pas être annulée.");
                msg.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
        msg.setWrapText(true);
        body.getChildren().addAll(warn, msg);
        root.getChildren().add(body);

        HBox btns = new HBox(10);
        btns.setPadding(new Insets(14, 20, 18, 20));
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:12px;" +
                "-fx-cursor:hand;-fx-background-radius:20;-fx-border-color:transparent;-fx-padding:8 18;");
        btnCancel.setOnAction(ev -> popup.close());
        Button btnDel = new Button("Supprimer définitivement");
        btnDel.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-background-radius:20;-fx-border-color:transparent;-fx-padding:8 18;");
        btnDel.setOnAction(ev -> {
            try {
                // Supprimer résultats, questions, puis évaluation
                PreparedStatement d1 = connection.prepareStatement(
                        "DELETE FROM resultatevaluation WHERE evaluation_id=?");
                d1.setInt(1, e.id); d1.executeUpdate();
                PreparedStatement d2 = connection.prepareStatement(
                        "DELETE FROM evaluation_question WHERE evaluation_id=?");
                d2.setInt(1, e.id); d2.executeUpdate();
                PreparedStatement d3 = connection.prepareStatement(
                        "DELETE FROM evaluationformation WHERE id=?");
                d3.setInt(1, e.id); d3.executeUpdate();
                popup.close();
                loadAll();
            } catch (SQLException ex) { showError("Erreur suppression", ex.getMessage()); }
        });
        btns.getChildren().addAll(btnCancel, btnDel);
        root.getChildren().add(btns);

        popup.setScene(new javafx.scene.Scene(root, 400, 240));
        popup.showAndWait();
    }

    private String getTimelineColor(EvalEntry e) {
        if (e.myStatut == MyStatut.PASSED) return "#10B981";
        if (e.myStatut == MyStatut.FAILED) return "#EF4444";
        return switch (e.accessState) {
            case ACCESSIBLE   -> "#6366F1";
            case LOCKED_LEVEL -> "#F59E0B";
            case NOT_YET      -> "#94A3B8";
            case EXPIRED      -> "#9CA3AF";
        };
    }

    private VBox buildHistoryPanel() {
        VBox panel=new VBox(7); panel.setPadding(new Insets(4,0,0,0));
        Label title=new Label("🕐  Historique de mes résultats"); title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111827;"); panel.getChildren().add(title);
        try {
            PreparedStatement ps=connection.prepareStatement("SELECT re.score_pct,re.datePassage,re.statut,IFNULL(re.niveau_delta,0) AS niveau_delta,IFNULL(ef.titre,f.titre) AS eval_titre FROM resultatevaluation re JOIN evaluationformation ef ON re.evaluation_id=ef.id JOIN sessionformation sf ON ef.session_id=sf.id JOIN formation f ON sf.formation_id=f.id WHERE re.employe_id=? ORDER BY re.datePassage DESC LIMIT 8");
            ps.setInt(1,currentUserId); ResultSet rs=ps.executeQuery(); boolean found=false;
            while (rs.next()) { found=true; panel.getChildren().add(buildHistoryRow(rs.getString("eval_titre"),rs.getInt("score_pct"),rs.getDate("datePassage"),rs.getString("statut"),rs.getInt("niveau_delta"))); }
            if (!found) { Label empty=new Label("Aucun résultat pour l'instant."); empty.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-padding:8 0;"); panel.getChildren().add(empty); }
        } catch (SQLException e) { System.err.println("history: "+e.getMessage()); }
        return panel;
    }

    private HBox buildHistoryRow(String titre, int score, java.sql.Date date, String statut, int delta) {
        HBox row=new HBox(9); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(8,12,8,10));
        row.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:#F1F5F9;-fx-border-radius:10;-fx-border-width:1;");
        boolean passed="passed".equals(statut); String color=passed?"#059669":"#DC2626", bgCol=passed?"#ECFDF5":"#FEF2F2";
        StackPane circle=new StackPane(); circle.setPrefSize(30,30); circle.setMinSize(30,30);
        circle.getChildren().addAll(new Circle(15,Color.web(bgCol)),makeBoldLabel(passed?"✓":"✗",color,"12px"));
        VBox info=new VBox(2); HBox.setHgrow(info,Priority.ALWAYS);
        String displayT=titre!=null?(titre.length()>28?titre.substring(0,26)+"…":titre):"—";
        info.getChildren().add(makeBoldLabel(displayT,"#1E293B","11px"));
        Label dl=new Label("📅 "+(date!=null?date.toLocalDate().format(FMT_FULL):"")); dl.setStyle("-fx-font-size:9px;-fx-text-fill:#94A3B8;"); info.getChildren().add(dl);
        VBox scoreBlock=new VBox(2); scoreBlock.setAlignment(Pos.CENTER_RIGHT);
        Label sl=new Label(score+"%"); sl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        HBox stars=new HBox(1); stars.setAlignment(Pos.CENTER); int starC=(int)Math.round(score/20.0);
        for (int i=1;i<=5;i++) { Label s=new Label(i<=starC?"★":"☆"); s.setStyle("-fx-font-size:8px;-fx-text-fill:"+(i<=starC?"#F59E0B":"#D1D5DB")+";"); stars.getChildren().add(s); }
        scoreBlock.getChildren().addAll(sl,stars);
        row.getChildren().addAll(circle,info,scoreBlock);
        if (delta!=0) { Label dlt=new Label((delta>0?"+":"")+delta); dlt.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:"+(delta>0?"#059669":"#DC2626")+";-fx-background-color:"+(delta>0?"#ECFDF5":"#FEF2F2")+";-fx-padding:3 7;-fx-background-radius:10;"); row.getChildren().add(dlt); }
        return row;
    }

    private Label makeBoldLabel(String t, String color, String size) { Label l=new Label(t); l.setStyle("-fx-font-size:"+size+";-fx-font-weight:bold;-fx-text-fill:"+color+";"); return l; }

    // ── Dialog dates ────────────────────────────────────────────────────
    private void openSetDatesDialog(EvalEntry e) {
        Dialog<ButtonType> d=new Dialog<>(); d.setTitle("Dates de l'évaluation");
        ButtonType ok=new ButtonType("Enregistrer",ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL); d.getDialogPane().setPrefWidth(400);
        VBox content=new VBox(12); content.setPadding(new Insets(18));
        String evalName=e.titre!=null&&!e.titre.isBlank()?e.titre:e.formationTitre;
        Label nl=new Label(evalName); nl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label sl2=new Label("Période pendant laquelle les employés peuvent passer cette évaluation."); sl2.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;"); sl2.setWrapText(true);
        GridPane g=new GridPane(); g.setHgap(12); g.setVgap(10);
        DatePicker dpD=new DatePicker(e.dateDebut!=null?e.dateDebut:LocalDate.now());
        DatePicker dpF=new DatePicker(e.dateFin!=null?e.dateFin:LocalDate.now().plusDays(7));
        dpD.setPrefWidth(160); dpF.setPrefWidth(160);
        g.add(new Label("Ouverture :"),0,0); g.add(dpD,1,0);
        g.add(new Label("Clôture :"),  0,1); g.add(dpF,1,1);
        content.getChildren().addAll(nl,sl2,new Separator(),g); d.getDialogPane().setContent(content);
        d.setResultConverter(btn->{
            if (btn==ok) {
                LocalDate debut=dpD.getValue(), fin=dpF.getValue();
                if (debut==null||fin==null) { showError("Erreur","Sélectionnez les deux dates."); return null; }
                if (fin.isBefore(debut)) { showError("Erreur","Clôture doit être après ouverture."); return null; }
                try { PreparedStatement ps=connection.prepareStatement("UPDATE evaluationformation SET dateDebut=?,dateFin=? WHERE id=?"); ps.setDate(1,java.sql.Date.valueOf(debut)); ps.setDate(2,java.sql.Date.valueOf(fin)); ps.setInt(3,e.id); ps.executeUpdate(); showInfo("Dates enregistrées","Du "+debut.format(FMT_FULL)+" au "+fin.format(FMT_FULL)); loadAll(); } catch (SQLException ex) { showError("Erreur",ex.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    // ── Quiz ────────────────────────────────────────────────────────────
    private void openQuiz(EvalEntry e) {
        List<QuizQuestion> questions=loadQuestions(e.id);
        if (questions.isEmpty()) { showInfo("Quiz vide","Aucune question n'a été ajoutée.\nAjoutez des lignes dans evaluation_question."); return; }
        Stage st=new Stage(); st.initOwner(vboxCampaigns.getScene().getWindow()); st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        st.setTitle("Évaluation — "+(e.titre!=null?e.titre:e.formationTitre));
        VBox root=new VBox(0); root.setStyle("-fx-background-color:#F8FAFF;");
        HBox hdr=new HBox(12); hdr.setPadding(new Insets(18,24,14,24)); hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color:linear-gradient(to right,#4F46E5,#7C3AED);");
        VBox hInfo=new VBox(3); HBox.setHgrow(hInfo,Priority.ALWAYS);
        Label hT=new Label(e.titre!=null?e.titre:e.formationTitre); hT.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hS=new Label(questions.size()+" questions · "+e.duree+" min · Score requis : "+e.scoreRequis+"%"); hS.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.75);");
        hInfo.getChildren().addAll(hT,hS);
        VBox impact=new VBox(3); impact.setAlignment(Pos.CENTER_RIGHT); impact.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-background-radius:8;-fx-padding:6 12;");
        Label iT=new Label("Impact niveau"); iT.setStyle("-fx-font-size:8px;-fx-text-fill:rgba(255,255,255,0.7);");
        HBox iR=new HBox(8); iR.setAlignment(Pos.CENTER);
        Label suc=new Label("✓ +"+e.niveauSucces+" niv."); suc.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#6EE7B7;");
        Label fai=new Label("✗ "+e.niveauEchec+" niv."); fai.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#FCA5A5;");
        iR.getChildren().addAll(suc,fai); impact.getChildren().addAll(iT,iR); hdr.getChildren().addAll(hInfo,impact);
        ScrollPane scroll=new ScrollPane(); scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:#F8FAFF;-fx-background-color:#F8FAFF;"); VBox.setVgrow(scroll,Priority.ALWAYS);
        VBox qContent=new VBox(12); qContent.setPadding(new Insets(16,24,16,24)); qContent.setStyle("-fx-background-color:#F8FAFF;");
        List<ToggleGroup> groups=new ArrayList<>();
        for (int i=0;i<questions.size();i++) groups.add(buildQBlock(qContent,questions.get(i),i+1,questions.size()));
        scroll.setContent(qContent);
        HBox footer=new HBox(12); footer.setPadding(new Insets(12,24,14,24)); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color:white;-fx-border-color:#E2E8F6 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        Button btnC=new Button("Annuler"); btnC.setStyle("-fx-background-color:transparent;-fx-text-fill:#6B7280;-fx-font-size:12px;-fx-cursor:hand;-fx-border-color:#E5E7EB;-fx-border-radius:18;-fx-background-radius:18;-fx-padding:8 20;"); btnC.setOnAction(ev->st.close());
        Button btnS=new Button("🏆  Soumettre"); btnS.setStyle("-fx-background-color:linear-gradient(to right,#4F46E5,#7C3AED);-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:8 24;-fx-background-radius:18;-fx-cursor:hand;");
        btnS.setOnAction(ev->{
            long unans=groups.stream().filter(gr->gr.getSelectedToggle()==null).count();
            if (unans>0) { showInfo("Questions manquantes","Répondez à toutes les questions ("+unans+" restante(s))."); return; }
            int correct=0;
            for (int i=0;i<questions.size();i++) { RadioButton sel=(RadioButton)groups.get(i).getSelectedToggle(); if (sel!=null&&sel.getUserData().equals(questions.get(i).bonneReponse)) correct++; }
            int score=correct*100/questions.size(); boolean passed=score>=e.scoreRequis;
            st.close(); showResult(e,score,passed,questions.size(),correct);
        });
        footer.getChildren().addAll(btnC,btnS);
        root.getChildren().addAll(hdr,scroll,footer);
        st.setScene(new javafx.scene.Scene(root,600,640)); st.show();
    }

    private ToggleGroup buildQBlock(VBox parent, QuizQuestion q, int num, int total) {
        VBox block=new VBox(7); block.setPadding(new Insets(14,15,14,15));
        block.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:#E2E8F6;-fx-border-radius:10;-fx-border-width:1;");
        HBox qh=new HBox(7); qh.setAlignment(Pos.CENTER_LEFT);
        Label nb=new Label("Q"+num); nb.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 7;-fx-background-radius:7;");
        Label pg=new Label(num+"/"+total); pg.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
        qh.getChildren().addAll(nb,pg); block.getChildren().add(qh);
        Label qt=new Label(q.question); qt.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#1E293B;"); qt.setWrapText(true); block.getChildren().add(qt);
        ToggleGroup tg=new ToggleGroup(); String[] letters={"A","B","C","D"}; String[] opts={q.optionA,q.optionB,q.optionC,q.optionD};
        for (int i=0;i<4;i++) {
            if (opts[i]==null||opts[i].isBlank()) continue;
            HBox opt=new HBox(7); opt.setAlignment(Pos.CENTER_LEFT); opt.setPadding(new Insets(7,10,7,10));
            opt.setStyle("-fx-background-color:#F8FAFF;-fx-background-radius:7;-fx-border-color:#E2E8F6;-fx-border-radius:7;-fx-border-width:1;-fx-cursor:hand;");
            Label badge=new Label(letters[i]); badge.setPrefWidth(19); badge.setMinWidth(19); badge.setAlignment(Pos.CENTER);
            badge.setStyle("-fx-background-color:#E2E8F6;-fx-text-fill:#475569;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 4;-fx-background-radius:5;");
            RadioButton rb=new RadioButton(opts[i]); rb.setToggleGroup(tg); rb.setUserData(letters[i]); rb.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;"); HBox.setHgrow(rb,Priority.ALWAYS);
            rb.selectedProperty().addListener((obs,o,sel)->{
                if (sel) { opt.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:7;-fx-border-color:#6366F1;-fx-border-radius:7;-fx-border-width:1.5;-fx-cursor:hand;"); badge.setStyle("-fx-background-color:#6366F1;-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 4;-fx-background-radius:5;"); }
                else { opt.setStyle("-fx-background-color:#F8FAFF;-fx-background-radius:7;-fx-border-color:#E2E8F6;-fx-border-radius:7;-fx-border-width:1;-fx-cursor:hand;"); badge.setStyle("-fx-background-color:#E2E8F6;-fx-text-fill:#475569;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 4;-fx-background-radius:5;"); }
            });
            opt.setOnMouseClicked(ev->rb.setSelected(true)); opt.getChildren().addAll(badge,rb); block.getChildren().add(opt);
        }
        parent.getChildren().add(block); return tg;
    }

    // ── Résultat ────────────────────────────────────────────────────────
    private void showResult(EvalEntry e, int score, boolean passed, int total, int correct) {
        int delta=passed?e.niveauSucces:e.niveauEchec;
        saveResult(e.id,score,passed,delta);
        if (e.competenceId>0) updateCompetenceLevel(e.competenceId,delta);
        int newLevel=getNiveauActuel(e.competenceId), maxLevel=getNiveauMax(e.competenceId);
        Dialog<Void> d=new Dialog<>(); d.setTitle("Résultat"); d.getDialogPane().getButtonTypes().add(ButtonType.OK); d.getDialogPane().setPrefWidth(440);
        VBox content=new VBox(14); content.setPadding(new Insets(24)); content.setAlignment(Pos.CENTER); content.setStyle("-fx-background-color:white;");
        Label ico=new Label(passed?"🏆":"📚"); ico.setStyle("-fx-font-size:44px;");
        Canvas sc=new Canvas(104,104); drawScoreCircle(sc,score,passed);
        Label res=new Label(passed?"Évaluation Réussie !":"Score Insuffisant"); res.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:"+(passed?"#059669":"#DC2626")+";");
        Label det=new Label(correct+" / "+total+" bonnes réponses"); det.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
        VBox impBox=new VBox(6); impBox.setAlignment(Pos.CENTER); impBox.setPadding(new Insets(12,20,12,20));
        impBox.setStyle("-fx-background-color:"+(delta>0?"#ECFDF5":"#FEF2F2")+";-fx-background-radius:10;");
        Label impT=new Label("Impact sur votre niveau de compétence"); impT.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;");
        Label impV=new Label((delta>0?"+":"")+delta+" niveau"+(Math.abs(delta)>1?"x":"")); impV.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:"+(delta>0?"#059669":"#DC2626")+";");
        impBox.getChildren().addAll(impT,impV);
        if (e.competenceId>0) {
            Label nl=new Label("Nouveau niveau : "+newLevel+" / "+maxLevel); nl.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;-fx-font-weight:bold;");
            HBox dotsRow=new HBox(4); dotsRow.setAlignment(Pos.CENTER);
            for (int i=1;i<=maxLevel;i++) { Circle c=new Circle(9); c.setFill(i<=newLevel?Color.web("#6366F1"):Color.web("#F1F5F9")); c.setStroke(i<=newLevel?Color.web("#4F46E5"):Color.web("#D1D5DB")); c.setStrokeWidth(1.5); dotsRow.getChildren().add(c); }
            impBox.getChildren().addAll(nl,dotsRow);
            if (passed&&newLevel<maxLevel) { Label unlock=new Label("🎉 Niveau "+(newLevel+1)+" débloqué !"); unlock.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#4F46E5;-fx-background-color:#EEF2FF;-fx-padding:4 12;-fx-background-radius:8;"); impBox.getChildren().add(unlock); }
            else if (passed&&newLevel>=maxLevel) { Label master=new Label("🏅 Niveau maximum atteint !"); master.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#059669;-fx-background-color:#ECFDF5;-fx-padding:4 12;-fx-background-radius:8;"); impBox.getChildren().add(master); }
        }
        content.getChildren().addAll(ico,sc,res,det,impBox);
        if (!passed) { Label hint=new Label("💡 Score requis : "+e.scoreRequis+"% — Réessayez après révision !"); hint.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;"); hint.setWrapText(true); content.getChildren().add(hint); }
        d.getDialogPane().setContent(content); d.showAndWait(); loadAll();
    }

    private void drawScoreCircle(Canvas c, int score, boolean passed) {
        GraphicsContext gc=c.getGraphicsContext2D(); double cx=52,cy=52,r=42; String col=passed?"#10B981":"#EF4444";
        gc.setStroke(Color.web("#F1F5F9")); gc.setLineWidth(7); gc.strokeOval(cx-r,cy-r,r*2,r*2);
        gc.setStroke(Color.web(col)); gc.setLineCap(StrokeLineCap.ROUND); gc.strokeArc(cx-r,cy-r,r*2,r*2,90,-(score*3.6),javafx.scene.shape.ArcType.OPEN);
        gc.setFill(Color.web(col)); gc.setFont(javafx.scene.text.Font.font("System",javafx.scene.text.FontWeight.BOLD,19)); gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER); gc.setTextBaseline(javafx.geometry.VPos.CENTER); gc.fillText(score+"%",cx,cy);
    }

    // ── DB ──────────────────────────────────────────────────────────────
    private void saveResult(int evalId, int score, boolean passed, int delta) {
        try {
            PreparedStatement chk=connection.prepareStatement("SELECT id FROM resultatevaluation WHERE evaluation_id=? AND employe_id=? LIMIT 1"); chk.setInt(1,evalId); chk.setInt(2,currentUserId); ResultSet rs=chk.executeQuery();
            if (rs.next()) { PreparedStatement u=connection.prepareStatement("UPDATE resultatevaluation SET note=?,score_pct=?,datePassage=?,statut=?,niveau_delta=? WHERE evaluation_id=? AND employe_id=?"); u.setDouble(1,score); u.setInt(2,score); u.setDate(3,java.sql.Date.valueOf(LocalDate.now())); u.setString(4,passed?"passed":"failed"); u.setInt(5,delta); u.setInt(6,evalId); u.setInt(7,currentUserId); u.executeUpdate(); }
            else { PreparedStatement i=connection.prepareStatement("INSERT INTO resultatevaluation(evaluation_id,employe_id,note,score_pct,datePassage,statut,niveau_delta) VALUES(?,?,?,?,?,?,?)"); i.setInt(1,evalId); i.setInt(2,currentUserId); i.setDouble(3,score); i.setInt(4,score); i.setDate(5,java.sql.Date.valueOf(LocalDate.now())); i.setString(6,passed?"passed":"failed"); i.setInt(7,delta); i.executeUpdate(); }
        } catch (SQLException e) { showError("Sauvegarde",e.getMessage()); }
    }

    private void updateCompetenceLevel(int competenceId, int delta) {
        if (delta==0||competenceId<=0) return;
        try {
            int maxLevel=getNiveauMax(competenceId);
            PreparedStatement ps=connection.prepareStatement("SELECT niveauActuel FROM competenceemploye WHERE employe_id=? AND competence_id=?"); ps.setInt(1,currentUserId); ps.setInt(2,competenceId); ResultSet rs=ps.executeQuery();
            if (rs.next()) { int cur=rs.getInt("niveauActuel"),nw=Math.max(0,Math.min(maxLevel,cur+delta)); PreparedStatement u=connection.prepareStatement("UPDATE competenceemploye SET niveauActuel=?,dateEvaluation=? WHERE employe_id=? AND competence_id=?"); u.setInt(1,nw); u.setDate(2,java.sql.Date.valueOf(LocalDate.now())); u.setInt(3,currentUserId); u.setInt(4,competenceId); u.executeUpdate(); }
            else { int nw=Math.max(0,Math.min(maxLevel,delta)); PreparedStatement i=connection.prepareStatement("INSERT INTO competenceemploye(niveauActuel,niveauValide,dateEvaluation,employe_id,competence_id) VALUES(?,0,?,?,?)"); i.setInt(1,nw); i.setDate(2,java.sql.Date.valueOf(LocalDate.now())); i.setInt(3,currentUserId); i.setInt(4,competenceId); i.executeUpdate(); }
        } catch (SQLException e) { System.err.println("updateLevel: "+e.getMessage()); }
    }

    private List<QuizQuestion> loadQuestions(int evalId) {
        List<QuizQuestion> list=new ArrayList<>();
        try { PreparedStatement ps=connection.prepareStatement("SELECT * FROM evaluation_question WHERE evaluation_id=? ORDER BY id"); ps.setInt(1,evalId); ResultSet rs=ps.executeQuery(); while (rs.next()) list.add(new QuizQuestion(rs.getString("question"),rs.getString("option_a"),rs.getString("option_b"),rs.getString("option_c"),rs.getString("option_d"),rs.getString("bonne_reponse"))); }
        catch (SQLException ignored) {}
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DÉTAILS
    // ══════════════════════════════════════════════════════════════════════
    private void openDetailDialog(EvalEntry e) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Détails — " + (e.titre != null && !e.titre.isBlank() ? e.titre : e.formationTitre));
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:white;");

        // Hero header
        String[] theme = getTheme(e);
        HBox hero = new HBox(14);
        hero.setPadding(new Insets(20, 24, 16, 24));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setStyle("-fx-background-color:linear-gradient(to right," + theme[0] + "22," + theme[0] + "08);");
        StackPane ico = new StackPane();
        ico.setPrefSize(54, 54); ico.setMinSize(54, 54);
        Circle icoBg = new Circle(27, Color.web(theme[0] + "33"));
        Label icoLbl = new Label(getTypeIcon(e.type)); icoLbl.setStyle("-fx-font-size:22px;");
        ico.getChildren().addAll(icoBg, icoLbl);
        VBox hInfo = new VBox(4); HBox.setHgrow(hInfo, Priority.ALWAYS);
        String titleStr = e.titre != null && !e.titre.isBlank() ? e.titre : e.formationTitre;
        Label ht = new Label(titleStr); ht.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        ht.setWrapText(true);
        Label hs = new Label("Formation : " + e.formationTitre);
        hs.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        hInfo.getChildren().addAll(ht, hs, buildStatusBadge(e));
        hero.getChildren().addAll(ico, hInfo);
        content.getChildren().add(hero);

        // Separator
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        content.getChildren().add(sep);

        // Details grid
        GridPane g = new GridPane();
        g.setHgap(16); g.setVgap(10);
        g.setPadding(new Insets(16, 24, 16, 24));

        int row = 0;
        addDetailRow(g, row++, "Type",       e.type != null ? e.type : "—");
        addDetailRow(g, row++, "Durée",      e.duree + " minutes");
        addDetailRow(g, row++, "Score requis", e.scoreRequis + "%");
        addDetailRow(g, row++, "Participants",
                e.nbDone + " terminés sur " + e.nbParticipants + " inscrits");

        if (e.dateDebut != null && e.dateFin != null) {
            addDetailRow(g, row++, "Ouverture", e.dateDebut.format(FMT_FULL));
            addDetailRow(g, row++, "Clôture",   e.dateFin.format(FMT_FULL));
            long duration = ChronoUnit.DAYS.between(e.dateDebut, e.dateFin);
            addDetailRow(g, row++, "Durée d'accès", duration + " jours");
        } else {
            addDetailRow(g, row++, "Période", "Non définie");
        }

        if (e.competenceId > 0) {
            String compName = getCompetenceName(e.competenceId);
            addDetailRow(g, row++, "Compétence", compName != null ? compName : "—");
            addDetailRow(g, row++, "Niveau requis", "Niveau " + e.niveauRequis + " minimum");
            addDetailRow(g, row++, "Niveau actuel", e.niveauActuel + " / " + e.niveauMax);
        }

        content.getChildren().add(g);

        // Impact niveaux
        HBox impact = new HBox(12);
        impact.setPadding(new Insets(10, 24, 10, 24));
        impact.setAlignment(Pos.CENTER_LEFT);
        impact.setStyle("-fx-background-color:#F8FAFF;-fx-border-color:#F1F5F9 transparent transparent transparent;-fx-border-width:1 0 0 0;");

        Label impLabel = new Label("Impact si :");
        impLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        Label okLbl = new Label("✓ Réussi → +" + e.niveauSucces + " niveau" + (e.niveauSucces > 1 ? "x" : ""));
        okLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#059669;" +
                "-fx-background-color:#ECFDF5;-fx-padding:4 12;-fx-background-radius:10;");
        Label koLbl = new Label("✗ Échoué → " + e.niveauEchec + " niveau" + (Math.abs(e.niveauEchec) > 1 ? "x" : ""));
        koLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#DC2626;" +
                "-fx-background-color:#FEF2F2;-fx-padding:4 12;-fx-background-radius:10;");
        impact.getChildren().addAll(impLabel, okLbl, koLbl);
        content.getChildren().add(impact);

        // Mon résultat si passé
        if (e.myStatut != MyStatut.NOT_TAKEN) {
            VBox myResult = new VBox(6);
            myResult.setPadding(new Insets(10, 24, 14, 24));
            myResult.setStyle("-fx-border-color:#F1F5F9 transparent transparent transparent;-fx-border-width:1 0 0 0;");
            Label mrt = new Label("Mon résultat");
            mrt.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#111827;");
            boolean passed = e.myStatut == MyStatut.PASSED;
            String rColor = passed ? "#059669" : "#DC2626";
            Label mrv = new Label((passed ? "✓ Réussi" : "✗ Échoué") + " — " + e.myScore + "%");
            mrv.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + rColor + ";");
            myResult.getChildren().addAll(mrt, mrv);
            content.getChildren().add(myResult);
        }

        d.getDialogPane().setContent(content);
        d.showAndWait();
    }

    private void addDetailRow(GridPane g, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;-fx-min-width:120;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        val.setWrapText(true);
        g.add(lbl, 0, row);
        g.add(val, 1, row);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODIFIER (ADMIN seulement)
    // ══════════════════════════════════════════════════════════════════════
    private void openEditDialog(EvalEntry e) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Modifier l'évaluation");
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        d.getDialogPane().setPrefWidth(520);

        ScrollPane sp = new ScrollPane(); sp.setFitToWidth(true); sp.setPrefHeight(500);
        GridPane g = new GridPane(); g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));

        // Pré-remplir les champs
        TextField tfT = new TextField(e.titre != null ? e.titre : "");
        tfT.setPromptText("Titre de l'évaluation"); tfT.setPrefWidth(270);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("QCM","Pratique","Oral","Projet","Auto-évaluation");
        cbType.setValue(e.type != null ? e.type : "QCM");

        Spinner<Integer> spD   = new Spinner<>(5, 180, e.duree);
        Spinner<Integer> spSc  = new Spinner<>(50, 100, e.scoreRequis);
        Spinner<Integer> spOk  = new Spinner<>(0, 5, Math.max(0, e.niveauSucces));
        Spinner<Integer> spKo  = new Spinner<>(-5, 0, Math.min(0, e.niveauEchec));

        DatePicker dpD = new DatePicker(e.dateDebut != null ? e.dateDebut : LocalDate.now());
        DatePicker dpF = new DatePicker(e.dateFin != null ? e.dateFin : LocalDate.now().plusDays(7));
        dpD.setPrefWidth(170); dpF.setPrefWidth(170);

        // Compétence liée
        ComboBox<String> cbC = new ComboBox<>();
        cbC.getItems().add("— Aucune —");
        Map<String, Integer> cMap = new LinkedHashMap<>();
        Map<String, Integer> mxMap = new LinkedHashMap<>();
        cMap.put("— Aucune —", 0); mxMap.put("— Aucune —", 0);
        String currentCompSel = "— Aucune —";
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT id,libelle,niveauMax FROM competence ORDER BY libelle").executeQuery();
            while (rs.next()) {
                String k = rs.getString("libelle") + " (max " + rs.getInt("niveauMax") + ")";
                cbC.getItems().add(k); cMap.put(k, rs.getInt("id")); mxMap.put(k, rs.getInt("niveauMax"));
                if (rs.getInt("id") == e.competenceId) currentCompSel = k;
            }
        } catch (SQLException ignored) {}
        cbC.setValue(currentCompSel);

        Spinner<Integer> spNR = new Spinner<>(0, Math.max(0, e.niveauMax - 1), e.niveauRequis);
        spNR.setDisable(e.competenceId == 0);
        cbC.setOnAction(ev -> {
            Integer mx = mxMap.getOrDefault(cbC.getValue(), 0);
            spNR.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, mx - 1), 0));
            spNR.setDisable(mx == 0);
        });

        // Difficulté
        ComboBox<String> cbDiff = new ComboBox<>();
        cbDiff.getItems().addAll("FACILE", "MOYEN", "DIFFICILE");
        cbDiff.setValue(e.difficulte != null ? e.difficulte : "MOYEN");
        Label diffHint = new Label(getDiffHint(cbDiff.getValue()));
        diffHint.setStyle("-fx-font-size:9px;-fx-text-fill:#6B7280;");
        cbDiff.setOnAction(ev -> diffHint.setText(getDiffHint(cbDiff.getValue())));

        int r = 0;
        g.add(new Label("Titre :"),            0, r); g.add(tfT,    1, r++);
        g.add(new Label("Type :"),             0, r); g.add(cbType, 1, r++);
        g.add(new Label("Difficulté :"),       0, r); g.add(new VBox(3, cbDiff, diffHint), 1, r++);
        g.add(new Label("Compétence liée :"),  0, r); g.add(cbC,    1, r++);
        g.add(new Label("Niveau requis :"),    0, r); g.add(spNR,   1, r++);
        g.add(new Label("Durée (min) :"),      0, r); g.add(spD,    1, r++);
        g.add(new Label("Score requis (%) :"), 0, r); g.add(spSc,   1, r++);
        g.add(new Label("Niveaux si réussi :"),0, r); g.add(spOk,   1, r++);
        g.add(new Label("Niveaux si échoué :"),0, r); g.add(spKo,   1, r++);
        g.add(new Separator(),                 0, r, 2, 1); r++;
        Label dtl = new Label("📅 Période de disponibilité");
        dtl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;");
        g.add(dtl,                             0, r, 2, 1); r++;
        g.add(new Label("Ouverture :"),        0, r); g.add(dpD, 1, r++);
        g.add(new Label("Clôture :"),          0, r); g.add(dpF, 1, r);

        sp.setContent(g);
        d.getDialogPane().setContent(sp);

        d.setResultConverter(btn -> {
            if (btn == ok) {
                try {
                    LocalDate debut = dpD.getValue(), fin = dpF.getValue();
                    if (debut != null && fin != null && fin.isBefore(debut)) {
                        showError("Dates", "La clôture doit être après l'ouverture."); return null;
                    }
                    int compId = cMap.getOrDefault(cbC.getValue(), 0);
                    int nivReq = spNR.isDisable() ? 0 : spNR.getValue();

                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE evaluationformation SET " +
                                    "titre=?, type=?, duree=?, score_requis=?, niveau_succes=?, niveau_echec=?, " +
                                    "competence_id=?, niveau_requis=?, dateDebut=?, dateFin=?, difficulte=? WHERE id=?"
                    );
                    ps.setString(1, tfT.getText());
                    ps.setString(2, cbType.getValue());
                    ps.setInt(3, spD.getValue());
                    ps.setInt(4, spSc.getValue());
                    ps.setInt(5, spOk.getValue());
                    ps.setInt(6, spKo.getValue());
                    if (compId > 0) ps.setInt(7, compId); else ps.setNull(7, Types.INTEGER);
                    ps.setInt(8, nivReq);
                    if (debut != null) ps.setDate(9, java.sql.Date.valueOf(debut));
                    else ps.setNull(9, Types.DATE);
                    if (fin != null) ps.setDate(10, java.sql.Date.valueOf(fin));
                    else ps.setNull(10, Types.DATE);
                    ps.setString(11, cbDiff.getValue());
                    ps.setInt(12, e.id);
                    ps.executeUpdate();
                    showInfo("✅ Modifié", "Évaluation mise à jour avec succès !");
                    loadAll();
                } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    @FXML
    private void handleNewCampaign() {
        Dialog<ButtonType> d=new Dialog<>(); d.setTitle("Nouvelle Évaluation");
        ButtonType ok=new ButtonType("Créer",ButtonBar.ButtonData.OK_DONE); d.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL); d.getDialogPane().setPrefWidth(500);
        ScrollPane sp=new ScrollPane(); sp.setFitToWidth(true); sp.setPrefHeight(480);
        GridPane g=new GridPane(); g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfT=new TextField(); tfT.setPromptText("Titre"); tfT.setPrefWidth(260);
        ComboBox<String> cbType=new ComboBox<>(); cbType.getItems().addAll("QCM","Pratique","Oral","Projet","Auto-évaluation"); cbType.setValue("QCM");
        Spinner<Integer> spD=new Spinner<>(5,180,30), spSc=new Spinner<>(50,100,70), spOk=new Spinner<>(0,5,1), spKo=new Spinner<>(-5,0,-1);
        ComboBox<String> cbS=new ComboBox<>(); Map<String,Integer> sMap=new LinkedHashMap<>();
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT sf.id, f.titre FROM sessionformation sf " +
                            "JOIN formation f ON sf.formation_id = f.id " +
                            "WHERE sf.id = (SELECT MAX(sf2.id) FROM sessionformation sf2 WHERE sf2.formation_id = f.id) " +
                            "ORDER BY f.titre ASC"
            ).executeQuery();
            while (rs.next()) {
                String k = rs.getString("titre");
                if (!sMap.containsKey(k)) { sMap.put(k, rs.getInt("id")); cbS.getItems().add(k); }
            }
            if (!cbS.getItems().isEmpty()) cbS.setValue(cbS.getItems().get(0));
        } catch (SQLException ignored) {}
        ComboBox<String> cbC=new ComboBox<>(); cbC.getItems().add("— Aucune —"); Map<String,Integer> cMap=new LinkedHashMap<>(), mxMap=new LinkedHashMap<>(); cMap.put("— Aucune —",0); mxMap.put("— Aucune —",0);
        try { ResultSet rs=connection.prepareStatement("SELECT id,libelle,niveauMax FROM competence ORDER BY libelle").executeQuery(); while (rs.next()) { String k=rs.getString("libelle")+" (max "+rs.getInt("niveauMax")+")"; cbC.getItems().add(k); cMap.put(k,rs.getInt("id")); mxMap.put(k,rs.getInt("niveauMax")); } } catch (SQLException ignored) {}
        cbC.setValue("— Aucune —");
        Spinner<Integer> spNR=new Spinner<>(0,0,0); spNR.setDisable(true);
        cbC.setOnAction(ev->{ Integer mx=mxMap.getOrDefault(cbC.getValue(),0); spNR.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,Math.max(0,mx-1),0)); spNR.setDisable(mx==0); });
        DatePicker dpD=new DatePicker(LocalDate.now()), dpF=new DatePicker(LocalDate.now().plusDays(7)); dpD.setPrefWidth(170); dpF.setPrefWidth(170);
        // Difficulté avec hint automatique
        ComboBox<String> cbDiff=new ComboBox<>();
        cbDiff.getItems().addAll("FACILE","MOYEN","DIFFICILE"); cbDiff.setValue("MOYEN");
        Label diffH=new Label(getDiffHint("MOYEN")); diffH.setStyle("-fx-font-size:9px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        cbDiff.setOnAction(ev->{
            diffH.setText(getDiffHint(cbDiff.getValue()));
            // Auto-ajuster score_requis selon difficulté
            if ("FACILE".equals(cbDiff.getValue())) { spSc.getValueFactory().setValue(60); spOk.getValueFactory().setValue(1); spKo.getValueFactory().setValue(-1); }
            else if ("DIFFICILE".equals(cbDiff.getValue())) { spSc.getValueFactory().setValue(80); spOk.getValueFactory().setValue(3); spKo.getValueFactory().setValue(-2); }
            else { spSc.getValueFactory().setValue(70); spOk.getValueFactory().setValue(2); spKo.getValueFactory().setValue(-1); }
        });

        int r=0;
        g.add(new Label("Titre :"),0,r); g.add(tfT,1,r++);
        g.add(new Label("Type :"),0,r); g.add(cbType,1,r++);
        g.add(new Label("Difficulté :"),0,r); g.add(new VBox(3,cbDiff,diffH),1,r++);
        g.add(new Label("Session :"),0,r); g.add(cbS,1,r++);
        g.add(new Label("Compétence liée :"),0,r); g.add(cbC,1,r++);
        g.add(new Label("Niveau requis :"),0,r); VBox nBox=new VBox(2,spNR,makeTinyLabel("Niveau min. pour accéder à cette évaluation")); g.add(nBox,1,r++);
        g.add(new Label("Durée (min) :"),0,r); g.add(spD,1,r++);
        g.add(new Label("Score requis (%) :"),0,r); g.add(spSc,1,r++);
        g.add(new Label("Niveaux si réussi :"),0,r); g.add(spOk,1,r++);
        g.add(new Label("Niveaux si échoué :"),0,r); g.add(spKo,1,r++);
        g.add(new Separator(),0,r,2,1); r++;
        Label dtl=new Label("📅 Période de disponibilité"); dtl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;"); g.add(dtl,0,r,2,1); r++;
        g.add(new Label("Ouverture :"),0,r); g.add(dpD,1,r++);
        g.add(new Label("Clôture :"),0,r); g.add(dpF,1,r);
        sp.setContent(g); d.getDialogPane().setContent(sp);
        d.setResultConverter(btn->{
            if (btn==ok) {
                try {
                    int sessionId=sMap.getOrDefault(cbS.getValue(),-1); if (sessionId<0) { showError("Erreur","Sélectionnez une session."); return null; }
                    int compId=cMap.getOrDefault(cbC.getValue(),0), nivReq=spNR.isDisable()?0:spNR.getValue();
                    LocalDate debut=dpD.getValue(), fin=dpF.getValue();
                    if (debut!=null&&fin!=null&&fin.isBefore(debut)) { showError("Dates","Clôture doit être après ouverture."); return null; }
                    PreparedStatement ps=connection.prepareStatement(
                            "INSERT INTO evaluationformation(titre,type,duree,session_id,score_requis,niveau_succes,niveau_echec,competence_id,niveau_requis,dateDebut,dateFin,difficulte) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");
                    ps.setString(1,tfT.getText()); ps.setString(2,cbType.getValue()); ps.setInt(3,spD.getValue()); ps.setInt(4,sessionId); ps.setInt(5,spSc.getValue()); ps.setInt(6,spOk.getValue()); ps.setInt(7,spKo.getValue());
                    if (compId>0) ps.setInt(8,compId); else ps.setNull(8,Types.INTEGER);
                    ps.setInt(9,nivReq);
                    if (debut!=null) ps.setDate(10,java.sql.Date.valueOf(debut)); else ps.setNull(10,Types.DATE);
                    if (fin!=null)   ps.setDate(11,java.sql.Date.valueOf(fin));   else ps.setNull(11,Types.DATE);
                    ps.setString(12,cbDiff.getValue());
                    ps.executeUpdate();
                    showInfo("✅ Évaluation créée","Créée avec succès ! ["+cbDiff.getValue()+"]\n"+
                            (compId>0?"Niveau requis : "+nivReq+"\n":"")+
                            (debut!=null?"Période : "+debut.format(FMT_FULL)+" → "+fin.format(FMT_FULL):""));
                    loadAll();
                } catch (Exception ex) { showError("Erreur",ex.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    private Label makeTinyLabel(String t) { Label l=new Label(t); l.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;"); return l; }

    private String[] getTheme(EvalEntry e) {
        if (e.myStatut==MyStatut.PASSED) return new String[]{"#10B981","#ECFDF5","#059669"};
        if (e.myStatut==MyStatut.FAILED) return new String[]{"#EF4444","#FEF2F2","#DC2626"};
        return switch(e.accessState){ case ACCESSIBLE->new String[]{"#6366F1","#EEF2FF","#4F46E5"}; case LOCKED_LEVEL->new String[]{"#F59E0B","#FEF3C7","#D97706"}; case NOT_YET->new String[]{"#94A3B8","#F8FAFC","#64748B"}; case EXPIRED->new String[]{"#9CA3AF","#F3F4F6","#6B7280"}; };
    }

    private String getTypeIcon(String type) { if (type==null) return "📋"; return switch(type.toLowerCase()){ case "qcm"->"📝"; case "pratique"->"🔧"; case "oral"->"🎤"; case "projet"->"🏗"; case "auto-évaluation","auto-evaluation"->"🪞"; default->"📋"; }; }

    private Label makePill(String text, String fg, String bg) { Label l=new Label(text); l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 9;-fx-background-radius:10;"); return l; }

    private void showError(String t, String m) { Alert a=new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
    private void showInfo(String t,  String m) { Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait(); }

    // ── Enums & Records ──────────────────────────────────────────────────
    enum DateStatus   { OPEN, NOT_YET, EXPIRED }
    enum AccessState  { ACCESSIBLE, LOCKED_LEVEL, NOT_YET, EXPIRED }
    enum MyStatut     { NOT_TAKEN, PASSED, FAILED }

    record EvalEntry(
            int id, String titre, String type, String formationTitre,
            LocalDate dateDebut, LocalDate dateFin,
            int nbParticipants, int nbDone,
            int scoreRequis, int niveauSucces, int niveauEchec,
            int competenceId, int niveauRequis, int niveauActuel, int niveauMax,
            int duree, DateStatus dateStatus, AccessState accessState, MyStatut myStatut, int myScore,
            String difficulte, String statutEval
    ) {}

    private static class QuizQuestion {
        String question,optionA,optionB,optionC,optionD,bonneReponse;
        QuizQuestion(String q,String a,String b,String c,String dd,String r){ question=q;optionA=a;optionB=b;optionC=c;optionD=dd;bonneReponse=r; }
    }
}