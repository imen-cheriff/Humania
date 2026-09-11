package planification.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Duration;
import planification.controllers.AjouterReuCont;
import planification.models.Espace;
import planification.models.Reunion;
import planification.services.ServiceEspace;
import planification.services.ServiceReunion;

import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.format.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class ReunionCalendarController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────
    @FXML
    private Button prevBtn, nextBtn, todayBtn, addBtn;
    @FXML
    private Label currentPeriodLabel;
    @FXML
    private ToggleButton dayBtn, weekBtn, monthBtn, yearBtn;
    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private GridPane miniCalGrid;
    @FXML
    private Label miniMonthLabel;
    @FXML
    private Button miniPrevBtn, miniNextBtn;
    @FXML
    private VBox upcomingList;
    @FXML
    private VBox calendarContainer;
    @FXML
    private Label entryCountLabel;
    @FXML
    private Label statusLabel;

    // ── Services ─────────────────────────────────────────────────────────
    private final ServiceReunion serviceReunion = new ServiceReunion();
    private final ServiceEspace serviceEspace = new ServiceEspace();

    // ── State ─────────────────────────────────────────────────────────────
    private enum ViewMode {
        DAY, WEEK, MONTH, YEAR
    }

    private ViewMode currentView = ViewMode.WEEK;
    private LocalDate currentDate = LocalDate.now();
    private LocalDate miniCalMonth = LocalDate.now().withDayOfMonth(1);

    private List<CalendarEntry> allEntries = new ArrayList<>();

    private Popup activePopup = null;

    private static final DateTimeFormatter FMT_MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FMT_DAY_FULL = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",
            Locale.FRENCH);

    private static final int HOUR_START = 7;
    private static final int HOUR_END = 21;
    private static final double HOUR_HEIGHT = 64.0;



    public static class CalendarEntry {

        public enum EntryType {
            MEETING, EVENT, RESERVATION, ONBOARDING, OFFBOARDING
        }

        public String title, subtitle;
        public Date start, end;
        public EntryType type;
        public boolean cancelled;
        public Reunion sourceReunion;

        public static CalendarEntry fromReunion(Reunion r) {
            CalendarEntry e = new CalendarEntry();
            e.title = r.getTitre() != null ? r.getTitre() : "(sans titre)";
            e.subtitle = r.getNomOrganisateur();
            e.start = r.getDateHeureDebut();
            e.end = r.getDateHeureFin();
            e.cancelled = !r.isStatut();
            e.type = detectType(r);
            e.sourceReunion = r;
            return e;
        }


        private static EntryType detectType(Reunion r) {
            String desc = r.getDescription() != null ? r.getDescription().trim().toUpperCase() : "";
            if (desc.startsWith("[EVENT]"))
                return EntryType.EVENT;
            if (desc.startsWith("[ONBOARDING]"))
                return EntryType.ONBOARDING;
            if (desc.startsWith("[OFFBOARDING]"))
                return EntryType.OFFBOARDING;
            if (desc.startsWith("[RESERVATION]"))
                return EntryType.RESERVATION;
            return EntryType.MEETING;
        }

        public String getColor() {
            if (sourceReunion != null && sourceReunion.isEnLigne()) {
                return "#ef4444"; // Réunions en ligne en rouge
            }
            if (cancelled) {
                return "#94a3b8";
            }
            switch (type) {
                case MEETING:
                    return "#ef4444"; // Red
                case EVENT:
                    return "#8b5cf6"; // Purple
                case RESERVATION:
                    return "#3b82f6"; // Blue
                case ONBOARDING:
                    return "#22c55e"; // Green
                case OFFBOARDING:
                    return "#f97316"; // Orange
                default:
                    return "#667eea";
            }
        }

        public String getLabel() {
            if (cancelled)
                return "Annulé";
            switch (type) {
                case MEETING:
                    return "Réunion";
                case EVENT:
                    return "Événement";
                case RESERVATION:
                    return "Réservation";
                case ONBOARDING:
                    return "Onboarding";
                case OFFBOARDING:
                    return "Offboarding";
                default:
                    return "";
            }
        }

        public String getIcon() {
            switch (type) {
                case MEETING:
                    return "📅";
                case EVENT:
                    return "🎉";
                case RESERVATION:
                    return "🪑";
                case ONBOARDING:
                    return "🟢";
                case OFFBOARDING:
                    return "🔴";
                default:
                    return "📌";
            }
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshData();
        renderAll();
    }

    private void loadReunionsFromDB() {
        allEntries.clear();
        try {
            for (Reunion r : serviceReunion.getAll()) {
                allEntries.add(CalendarEntry.fromReunion(r));
            }
        } catch (Exception ignored) {
        }
    }


    private void refreshData() {
        loadReunionsFromDB();
    }


    @FXML
    private void handlePrev() {
        switch (currentView) {
            case DAY:
                currentDate = currentDate.minusDays(1);
                break;
            case WEEK:
                currentDate = currentDate.minusWeeks(1);
                break;
            case MONTH:
                currentDate = currentDate.minusMonths(1);
                break;
            case YEAR:
                currentDate = currentDate.minusYears(1);
                break;
        }
        dismissPopup();
        renderAll();
    }

    @FXML
    private void handleNext() {
        switch (currentView) {
            case DAY:
                currentDate = currentDate.plusDays(1);
                break;
            case WEEK:
                currentDate = currentDate.plusWeeks(1);
                break;
            case MONTH:
                currentDate = currentDate.plusMonths(1);
                break;
            case YEAR:
                currentDate = currentDate.plusYears(1);
                break;
        }
        dismissPopup();
        renderAll();
    }

    @FXML
    private void handleToday() {
        currentDate = LocalDate.now();
        dismissPopup();
        renderAll();
    }

    @FXML
    private void handleViewChange() {
        Toggle sel = viewToggleGroup.getSelectedToggle();
        if (sel == dayBtn)
            currentView = ViewMode.DAY;
        else if (sel == weekBtn)
            currentView = ViewMode.WEEK;
        else if (sel == monthBtn)
            currentView = ViewMode.MONTH;
        else if (sel == yearBtn)
            currentView = ViewMode.YEAR;
        styleViewButtons();
        dismissPopup();
        renderAll();
    }

    @FXML
    private void handleAdd() {
        openForm(null, currentDate.atTime(9, 0));
    }

    @FXML
    private void handleMiniPrev() {
        miniCalMonth = miniCalMonth.minusMonths(1);
        renderMiniCalendar();
    }

    @FXML
    private void handleMiniNext() {
        miniCalMonth = miniCalMonth.plusMonths(1);
        renderMiniCalendar();
    }


    private void renderAll() {
        updatePeriodLabel();
        renderMiniCalendar();
        renderLegend();
        renderUpcoming();
        renderCalendar();
        styleViewButtons();
        // Update entry count label in toolbar
        if (entryCountLabel != null) {
            entryCountLabel.setText(allEntries.size() + " entrée" + (allEntries.size() > 1 ? "s" : ""));
        }
    }

    private void updatePeriodLabel() {
        switch (currentView) {
            case DAY:
                currentPeriodLabel.setText(capitalize(currentDate.format(FMT_DAY_FULL)));
                break;
            case WEEK: {
                LocalDate mon = currentDate.with(DayOfWeek.MONDAY);
                LocalDate sun = mon.plusDays(6);
                if (mon.getMonth() == sun.getMonth())
                    currentPeriodLabel.setText(mon.getDayOfMonth() + " – " + sun.getDayOfMonth() + " "
                            + capitalize(mon.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))));
                else
                    currentPeriodLabel.setText(mon.getDayOfMonth() + " "
                            + capitalize(mon.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)))
                            + " – " + sun.getDayOfMonth() + " "
                            + capitalize(sun.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH))));
                break;
            }
            case MONTH:
                currentPeriodLabel.setText(capitalize(currentDate.format(FMT_MONTH_YEAR)));
                break;
            case YEAR:
                currentPeriodLabel.setText(String.valueOf(currentDate.getYear()));
                break;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Mini calendar (left sidebar)
    // ═════════════════════════════════════════════════════════════════════

    private void renderMiniCalendar() {
        miniCalGrid.getChildren().clear();
        miniMonthLabel.setText(capitalize(miniCalMonth.format(FMT_MONTH_YEAR)));

        String[] dayNames = { "Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di" };
        for (int i = 0; i < 7; i++) {
            Label h = new Label(dayNames[i]);
            h.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;" +
                    " -fx-min-width: 24; -fx-alignment: CENTER;");
            miniCalGrid.add(h, i, 0);
        }

        // Collect dates that have entries (coloured dots per type)
        Map<LocalDate, String> dotColors = new HashMap<>();
        for (CalendarEntry e : allEntries) {
            if (e.start == null)
                continue;
            LocalDate d = toLocalDate(e.start);
            dotColors.putIfAbsent(d, e.getColor());
        }

        LocalDate first = miniCalMonth;
        int offset = first.getDayOfWeek().getValue() - 1;
        LocalDate day = first.minusDays(offset);

        for (int week = 0; week < 6; week++) {
            for (int d = 0; d < 7; d++) {
                final LocalDate cellDay = day;
                boolean curMonth = cellDay.getMonth() == miniCalMonth.getMonth();
                boolean isToday = cellDay.equals(LocalDate.now());
                String dotColor = dotColors.get(cellDay);

                StackPane cell = new StackPane();
                cell.setMinSize(24, 24);

                if (isToday) {
                    Circle bg = new Circle(11);
                    bg.setFill(Color.web("#667eea"));
                    cell.getChildren().add(bg);
                } else if (dotColor != null) {
                    Circle bg = new Circle(11);
                    bg.setFill(Color.web(dotColor + "33"));
                    cell.getChildren().add(bg);
                }

                // Dot indicator at bottom-right when has events and not today
                if (dotColor != null && !isToday) {
                    Circle dot = new Circle(3);
                    dot.setFill(Color.web(dotColor));
                    StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
                    StackPane.setMargin(dot, new Insets(0, 1, 1, 0));
                    cell.getChildren().add(dot);
                }

                Label lbl = new Label(String.valueOf(cellDay.getDayOfMonth()));
                lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: " + (isToday ? "bold" : "normal")
                        + "; -fx-text-fill: " + (isToday ? "white" : curMonth ? "#1e293b" : "#cbd5e1") + ";");
                cell.getChildren().add(lbl);
                cell.setCursor(Cursor.HAND);

                cell.setOnMouseClicked(e -> {
                    currentDate = cellDay;
                    miniCalMonth = cellDay.withDayOfMonth(1);
                    if (currentView == ViewMode.MONTH || currentView == ViewMode.YEAR)
                        currentView = ViewMode.MONTH;
                    renderAll();
                });

                miniCalGrid.add(cell, d, week + 1);
                day = day.plusDays(1);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Colour legend
    // ═════════════════════════════════════════════════════════════════════

    private void renderLegend() {
        // legendBox was removed from FXML (legend is now an inline strip in the
        // toolbar).
        // This method is kept as a no-op for safety.
    }

    // ═════════════════════════════════════════════════════════════════════
    // Upcoming list (left sidebar)
    // ═════════════════════════════════════════════════════════════════════

    private void renderUpcoming() {
        upcomingList.getChildren().clear();
        LocalDate today = LocalDate.now();

        List<CalendarEntry> upcoming = allEntries.stream()
                .filter(e -> e.start != null)
                .filter(e -> !toLocalDate(e.start).isBefore(today))
                .sorted(Comparator.comparing(e -> e.start))
                .limit(8)
                .collect(Collectors.toList());

        for (CalendarEntry e : upcoming) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 6, 5, 6));
            row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");
            row.setOnMouseEntered(
                    ev -> row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6; -fx-cursor: hand;"));
            row.setOnMouseExited(ev -> row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));

            // Coloured left bar
            Rectangle bar = new Rectangle(3, 28);
            bar.setArcWidth(3);
            bar.setArcHeight(3);
            bar.setFill(Color.web(e.getColor()));

            VBox info = new VBox(1);
            Label title = new Label(e.title);
            title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            title.setMaxWidth(150);
            title.setWrapText(false);

            String timeStr = formatDate(e.start) + " · " + formatTime(e.start) + "–" + formatTime(e.end);
            Label time = new Label(timeStr);
            time.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b;");

            Label badge = new Label(e.getLabel());
            badge.setStyle("-fx-font-size: 9px; -fx-text-fill: " + e.getColor() + "; " +
                    "-fx-background-color: " + e.getColor() + "22; " +
                    "-fx-background-radius: 4; -fx-padding: 1 4;");

            info.getChildren().addAll(title, time, badge);
            row.getChildren().addAll(bar, info);

            final CalendarEntry entry = e;
            row.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    currentDate = toLocalDate(entry.start);
                    currentView = ViewMode.DAY;
                    dayBtn.setSelected(true);
                    styleViewButtons();
                    renderAll();
                }
            });
            upcomingList.getChildren().add(row);
        }

        if (upcoming.isEmpty()) {
            Label empty = new Label("Aucun événement à venir");
            empty.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 6 0 0 0;");
            upcomingList.getChildren().add(empty);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Calendar router
    // ═════════════════════════════════════════════════════════════════════

    private void renderCalendar() {
        calendarContainer.getChildren().clear();
        switch (currentView) {
            case DAY:
                renderDayView(currentDate);
                break;
            case WEEK:
                renderWeekView();
                break;
            case MONTH:
                renderMonthView();
                break;
            case YEAR:
                renderYearView();
                break;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // WEEK VIEW
    // ═════════════════════════════════════════════════════════════════════

    private void renderWeekView() {
        LocalDate monday = currentDate.with(DayOfWeek.MONDAY);

        // ── Column headers ────────────────────────────────────────────────
        HBox headerRow = new HBox(0);
        headerRow.setStyle("-fx-background-color: white; " +
                "-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        // Week-number gutter
        int weekNum = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
        VBox weekNumBox = new VBox();
        weekNumBox.setPrefWidth(58);
        weekNumBox.setAlignment(Pos.CENTER);
        Label wnLbl = new Label("W" + weekNum);
        wnLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #cbd5e1; -fx-padding: 4 0 4 0;");
        weekNumBox.getChildren().add(wnLbl);
        headerRow.getChildren().add(weekNumBox);

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            boolean isToday = day.equals(LocalDate.now());

            VBox cell = new VBox(2);
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(0);
            HBox.setHgrow(cell, Priority.ALWAYS);
            cell.setPadding(new Insets(8, 4, 8, 4));
            cell.setStyle("-fx-border-color: transparent #e2e8f0 transparent transparent; -fx-border-width: 0 1 0 0;"
                    + (isToday ? " -fx-background-color: #f5f3ff;" : ""));

            String dayName = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            Label dayLbl = new Label(capitalize(dayName));
            dayLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isToday ? "#667eea" : "#94a3b8") + ";");

            StackPane numPane = new StackPane();
            if (isToday) {
                Circle bg = new Circle(16);
                bg.setFill(Color.web("#667eea"));
                numPane.getChildren().add(bg);
            }
            Label numLbl = new Label(String.valueOf(day.getDayOfMonth()));
            numLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " +
                    (isToday ? "white" : "#1e293b") + ";");
            numPane.getChildren().add(numLbl);
            cell.getChildren().addAll(dayLbl, numPane);

            final LocalDate fd = day;
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> {
                currentDate = fd;
                currentView = ViewMode.DAY;
                dayBtn.setSelected(true);
                styleViewButtons();
                renderAll();
            });
            headerRow.getChildren().add(cell);
        }
        calendarContainer.getChildren().add(headerRow);

        // ── Time grid ────────────────────────────────────────────────────
        double totalH = (HOUR_END - HOUR_START) * HOUR_HEIGHT;

        // Time label column
        Pane timeCol = buildTimeColumn(totalH);

        // Day columns
        HBox daysRow = new HBox(0);
        daysRow.setStyle("-fx-background-color: white;");
        daysRow.getChildren().add(timeCol);

        // Group entries by day
        Map<LocalDate, List<CalendarEntry>> byDay = new HashMap<>();
        for (int i = 0; i < 7; i++)
            byDay.put(monday.plusDays(i), new ArrayList<>());
        for (CalendarEntry e : allEntries) {
            if (e.start == null)
                continue;
            LocalDate d = toLocalDate(e.start);
            if (byDay.containsKey(d))
                byDay.get(d).add(e);
        }

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            boolean isToday = day.equals(LocalDate.now());
            List<CalendarEntry> dayEntries = byDay.get(day);

            Pane dayPane = new Pane();
            dayPane.setMinHeight(totalH);
            HBox.setHgrow(dayPane, Priority.ALWAYS);
            dayPane.setStyle("-fx-background-color: " + (isToday ? "#fafafe" : "white") + "; "
                    + "-fx-border-color: transparent #e2e8f0 transparent transparent; -fx-border-width: 0 1 0 0;");

            drawHourLines(dayPane, totalH);
            if (isToday)
                drawNowLine(dayPane);

            // Double-click to create
            final LocalDate fd = day;
            dayPane.setOnMouseClicked(e -> {
                dismissPopup();
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    int hour = (int) (e.getY() / HOUR_HEIGHT) + HOUR_START;
                    int min = snapMinute((int) ((e.getY() % HOUR_HEIGHT) / HOUR_HEIGHT * 60));
                    openForm(null, fd.atTime(Math.min(hour, HOUR_END - 1), min));
                }
            });

            // Draw entries with overlap handling
            drawEntriesOnPane(dayPane, dayEntries, true);
            daysRow.getChildren().add(dayPane);
        }

        ScrollPane scroll = styledScroll(daysRow);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scrollToHour(scroll, totalH, 8);
        calendarContainer.getChildren().add(scroll);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DAY VIEW
    // ═════════════════════════════════════════════════════════════════════

    private void renderDayView(LocalDate day) {
        boolean isToday = day.equals(LocalDate.now());

        // Header
        VBox header = new VBox(2);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: white; " +
                "-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        Label nameLbl = new Label(capitalize(day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH)));
        nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isToday ? "#667eea" : "#94a3b8") + ";");

        StackPane numPane = new StackPane();
        numPane.setAlignment(Pos.CENTER);
        if (isToday) {
            Circle bg = new Circle(22);
            bg.setFill(Color.web("#667eea"));
            numPane.getChildren().add(bg);
        }
        Label numLbl = new Label(String.valueOf(day.getDayOfMonth()));
        numLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " +
                (isToday ? "white" : "#1e293b") + ";");
        numPane.getChildren().add(numLbl);
        header.getChildren().addAll(nameLbl, numPane);
        calendarContainer.getChildren().add(header);

        // Time grid
        double totalH = (HOUR_END - HOUR_START) * HOUR_HEIGHT;
        Pane timeCol = buildTimeColumn(totalH);

        Pane evPane = new Pane();
        evPane.setMinHeight(totalH);
        HBox.setHgrow(evPane, Priority.ALWAYS);
        evPane.setStyle("-fx-background-color: " + (isToday ? "#fafafe" : "white") + ";");

        drawHourLines(evPane, totalH);
        if (isToday)
            drawNowLine(evPane);

        evPane.setOnMouseClicked(e -> {
            dismissPopup();
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                int hour = (int) (e.getY() / HOUR_HEIGHT) + HOUR_START;
                int min = snapMinute((int) ((e.getY() % HOUR_HEIGHT) / HOUR_HEIGHT * 60));
                openForm(null, day.atTime(Math.min(hour, HOUR_END - 1), min));
            }
        });

        List<CalendarEntry> dayEntries = allEntries.stream()
                .filter(e -> e.start != null && toLocalDate(e.start).equals(day))
                .sorted(Comparator.comparing(e -> e.start))
                .collect(Collectors.toList());

        drawEntriesOnPane(evPane, dayEntries, true);

        HBox row = new HBox(0);
        row.setStyle("-fx-background-color: white;");
        row.getChildren().addAll(timeCol, evPane);

        ScrollPane scroll = styledScroll(row);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scrollToHour(scroll, totalH, 8);
        calendarContainer.getChildren().add(scroll);
    }

    // ═════════════════════════════════════════════════════════════════════
    // MONTH VIEW
    // ═════════════════════════════════════════════════════════════════════

    private void renderMonthView() {
        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int offset = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate start = firstOfMonth.minusDays(offset);

        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color: white;");
        grid.setVgap(0);
        grid.setHgap(0);
        VBox.setVgrow(grid, Priority.ALWAYS);

        // Day-of-week headers
        String[] dayNames = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };
        for (int i = 0; i < 7; i++) {
            Label h = new Label(dayNames[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            h.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; " +
                    "-fx-padding: 8 0 8 0; -fx-border-color: transparent transparent #e2e8f0 " +
                    (i < 6 ? "#e2e8f0" : "transparent") + "; -fx-border-width: 0 " + (i < 6 ? 1 : 0) + " 1 0;" +
                    "-fx-background-color: white;");
            GridPane.setHgrow(h, Priority.ALWAYS);
            grid.add(h, i, 0);
        }

        // Group entries by date
        Map<LocalDate, List<CalendarEntry>> byDay = new HashMap<>();
        for (CalendarEntry e : allEntries) {
            if (e.start == null)
                continue;
            byDay.computeIfAbsent(toLocalDate(e.start), k -> new ArrayList<>()).add(e);
        }

        LocalDate day = start;
        for (int week = 0; week < 6; week++) {
            for (int d = 0; d < 7; d++) {
                final LocalDate cellDay = day;
                boolean curMonth = cellDay.getMonth() == firstOfMonth.getMonth();
                boolean isToday = cellDay.equals(LocalDate.now());
                List<CalendarEntry> events = byDay.getOrDefault(cellDay, List.of());

                VBox cell = new VBox(2);
                cell.setPadding(new Insets(4, 5, 4, 5));
                cell.setPrefHeight(95);
                cell.setMaxHeight(95);
                cell.setStyle("-fx-background-color: " + (isToday ? "#f5f3ff" : "white") + "; " +
                        "-fx-border-color: #e2e8f0; -fx-border-width: 0 " + (d < 6 ? 1 : 0)
                        + " 1 0; -fx-cursor: hand;");
                GridPane.setVgrow(cell, Priority.ALWAYS);
                GridPane.setHgrow(cell, Priority.ALWAYS);

                // Day number
                StackPane numPane = new StackPane();
                numPane.setAlignment(Pos.TOP_LEFT);
                numPane.setMaxWidth(Double.MAX_VALUE);
                if (isToday) {
                    Circle bg = new Circle(12);
                    bg.setFill(Color.web("#667eea"));
                    StackPane.setAlignment(bg, Pos.TOP_LEFT);
                    numPane.getChildren().add(bg);
                }
                Label numLbl = new Label(String.valueOf(cellDay.getDayOfMonth()));
                numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (isToday ? "bold" : "normal")
                        + "; -fx-text-fill: " + (isToday ? "white" : curMonth ? "#1e293b" : "#cbd5e1") + ";");
                if (isToday)
                    StackPane.setAlignment(numLbl, Pos.TOP_LEFT);
                numPane.getChildren().add(numLbl);
                cell.getChildren().add(numPane);

                // Show up to 3 events, then "+N more"
                int shown = Math.min(events.size(), 3);
                for (int ei = 0; ei < shown; ei++) {
                    CalendarEntry ev = events.get(ei);
                    HBox pill = new HBox(3);
                    pill.setAlignment(Pos.CENTER_LEFT);
                    pill.setStyle("-fx-background-color: " + ev.getColor() + "22; " +
                            "-fx-background-radius: 3; -fx-padding: 1 5;");
                    Circle dot = new Circle(4);
                    dot.setFill(Color.web(ev.getColor()));
                    Label evLbl = new Label(ev.title);
                    evLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + ev.getColor() + ";");
                    evLbl.setMaxWidth(Double.MAX_VALUE);
                    evLbl.setWrapText(false);
                    pill.getChildren().addAll(dot, evLbl);
                    pill.setMaxWidth(Double.MAX_VALUE);
                    final CalendarEntry fe = ev;
                    pill.setOnMouseClicked(e -> {
                        e.consume();
                        showQuickPopup(fe, pill);
                    });
                    cell.getChildren().add(pill);
                }
                if (events.size() > 3) {
                    Label more = new Label("+" + (events.size() - 3) + " autres");
                    more.setStyle("-fx-font-size: 9px; -fx-text-fill: #667eea; -fx-padding: 0 0 0 4;");
                    cell.getChildren().add(more);
                }

                cell.setOnMouseClicked(e -> {
                    dismissPopup();
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                        currentDate = cellDay;
                        currentView = ViewMode.DAY;
                        dayBtn.setSelected(true);
                        styleViewButtons();
                        renderAll();
                    }
                });

                grid.add(cell, d, week + 1);
                day = day.plusDays(1);
            }
        }

        ScrollPane scroll = styledScroll(grid);
        scroll.setFitToHeight(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        calendarContainer.getChildren().add(scroll);
    }

    // ═════════════════════════════════════════════════════════════════════
    // YEAR VIEW — 3 rows × 4 columns of mini-month grids
    // ═════════════════════════════════════════════════════════════════════

    private void renderYearView() {
        int year = currentDate.getYear();

        // Header
        Label yearHeader = new Label(String.valueOf(year));
        yearHeader.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;" +
                " -fx-padding: 12 20 8 20;");
        calendarContainer.getChildren().add(yearHeader);

        // Group entries by date
        Map<LocalDate, List<CalendarEntry>> byDay = new HashMap<>();
        for (CalendarEntry e : allEntries) {
            if (e.start == null)
                continue;
            LocalDate d = toLocalDate(e.start);
            if (d.getYear() == year)
                byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
        }

        // 3 rows × 4 months
        GridPane yearGrid = new GridPane();
        yearGrid.setHgap(12);
        yearGrid.setVgap(12);
        yearGrid.setPadding(new Insets(4, 16, 16, 16));
        VBox.setVgrow(yearGrid, Priority.ALWAYS);

        for (int col = 0; col < 4; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            yearGrid.getColumnConstraints().add(cc);
        }

        for (int m = 1; m <= 12; m++) {
            LocalDate monthFirst = LocalDate.of(year, m, 1);
            VBox monthBlock = buildYearMonthBlock(monthFirst, byDay);

            int row = (m - 1) / 4;
            int col = (m - 1) % 4;
            yearGrid.add(monthBlock, col, row);
        }

        ScrollPane scroll = styledScroll(yearGrid);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        calendarContainer.getChildren().add(scroll);
    }

    /** Builds a single mini-month block for the year view. */
    private VBox buildYearMonthBlock(LocalDate monthFirst, Map<LocalDate, List<CalendarEntry>> byDay) {
        VBox block = new VBox(6);
        block.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-padding: 10;");
        block.setCursor(Cursor.HAND);
        block.setOnMouseClicked(e -> {
            currentDate = monthFirst;
            currentView = ViewMode.MONTH;
            monthBtn.setSelected(true);
            styleViewButtons();
            renderAll();
        });

        // Month title
        Label title = new Label(capitalize(monthFirst.format(
                DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH))));
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        boolean isCurrentMonth = monthFirst.getMonth() == LocalDate.now().getMonth()
                && monthFirst.getYear() == LocalDate.now().getYear();
        if (isCurrentMonth)
            title.setStyle(title.getStyle() + " -fx-text-fill: #667eea;");

        block.getChildren().add(title);

        // Mini grid
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);

        String[] dayNames = { "L", "M", "M", "J", "V", "S", "D" };
        for (int i = 0; i < 7; i++) {
            Label h = new Label(dayNames[i]);
            h.setStyle("-fx-font-size: 8px; -fx-text-fill: #94a3b8; -fx-min-width: 20; -fx-alignment: CENTER;");
            grid.add(h, i, 0);
        }

        int offset = monthFirst.getDayOfWeek().getValue() - 1;
        LocalDate day = monthFirst.minusDays(offset);

        for (int week = 0; week < 6; week++) {
            for (int d = 0; d < 7; d++) {
                final LocalDate cellDay = day;
                boolean curMonth = cellDay.getMonth() == monthFirst.getMonth();
                boolean isToday = cellDay.equals(LocalDate.now());
                List<CalendarEntry> evs = byDay.getOrDefault(cellDay, List.of());

                StackPane cell = new StackPane();
                cell.setMinSize(20, 20);

                if (isToday) {
                    Circle bg = new Circle(9);
                    bg.setFill(Color.web("#667eea"));
                    cell.getChildren().add(bg);
                } else if (!evs.isEmpty() && curMonth) {
                    // Show coloured dot for dominant event type
                    String dotCol = evs.get(0).getColor();
                    Circle bg = new Circle(9);
                    bg.setFill(Color.web(dotCol + "33"));
                    Circle dt = new Circle(3);
                    dt.setFill(Color.web(dotCol));
                    StackPane.setAlignment(dt, Pos.BOTTOM_RIGHT);
                    cell.getChildren().addAll(bg, dt);
                }

                Label lbl = new Label(String.valueOf(cellDay.getDayOfMonth()));
                lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: "
                        + (isToday ? "white" : curMonth ? "#1e293b" : "#e2e8f0") + ";");
                cell.getChildren().add(lbl);

                cell.setOnMouseClicked(e -> {
                    e.consume();
                    currentDate = cellDay;
                    currentView = ViewMode.DAY;
                    dayBtn.setSelected(true);
                    styleViewButtons();
                    renderAll();
                });

                grid.add(cell, d, week + 1);
                day = day.plusDays(1);
            }
        }

        block.getChildren().add(grid);

        // Event count badge
        long cnt = byDay.entrySet().stream()
                .filter(e -> e.getKey().getMonth() == monthFirst.getMonth())
                .mapToLong(e -> e.getValue().size()).sum();
        if (cnt > 0) {
            Label badge = new Label(cnt + " événement" + (cnt > 1 ? "s" : ""));
            badge.setStyle("-fx-font-size: 9px; -fx-text-fill: #667eea; " +
                    "-fx-background-color: #ede9fe; -fx-background-radius: 4; -fx-padding: 2 5;");
            block.getChildren().add(badge);
        }

        return block;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Event block builder (Day & Week time grid)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Draw all entries on a time-grid Pane with basic overlap column splitting.
     */
    private void drawEntriesOnPane(Pane pane, List<CalendarEntry> entries, boolean inTimeGrid) {
        if (entries.isEmpty())
            return;

        // Simple overlap: sort by start, assign column indices
        List<List<CalendarEntry>> columns = new ArrayList<>();
        for (CalendarEntry e : entries) {
            boolean placed = false;
            for (List<CalendarEntry> col : columns) {
                CalendarEntry last = col.get(col.size() - 1);
                if (e.start != null && last.end != null && !e.start.before(last.end)) {
                    col.add(e);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<CalendarEntry> col = new ArrayList<>();
                col.add(e);
                columns.add(col);
            }
        }

        int totalCols = columns.size();

        for (int ci = 0; ci < totalCols; ci++) {
            for (CalendarEntry e : columns.get(ci)) {
                if (e.start == null)
                    continue;
                VBox ev = buildEventBlock(e, inTimeGrid);

                LocalTime start = toLocalTime(e.start);
                LocalTime end = e.end != null ? toLocalTime(e.end) : start.plusHours(1);
                double startY = (start.getHour() - HOUR_START + start.getMinute() / 60.0) * HOUR_HEIGHT;
                double endY = (end.getHour() - HOUR_START + end.getMinute() / 60.0) * HOUR_HEIGHT;
                double evH = Math.max(endY - startY, 24);

                ev.setLayoutY(startY + 1);
                ev.setPrefHeight(evH - 2);

                final int fci = ci;
                final int ftotCols = totalCols;
                pane.widthProperty().addListener((obs, o, w) -> {
                    double colW = w.doubleValue() / ftotCols;
                    ev.setLayoutX(fci * colW + 4);
                    ev.setPrefWidth(colW - 8);
                });
                // initial size
                if (pane.getWidth() > 0) {
                    double colW = pane.getWidth() / totalCols;
                    ev.setLayoutX(ci * colW + 4);
                    ev.setPrefWidth(colW - 8);
                }

                pane.getChildren().add(ev);
            }
        }
    }

    private VBox buildEventBlock(CalendarEntry entry, boolean inTimeGrid) {
        String color = entry.getColor();
        VBox box = new VBox(1);
        box.setPadding(new Insets(3, 6, 3, 8));
        box.setStyle(eventBlockStyle(color, false));

        // Type badge chip
        Label badge = new Label(entry.getIcon() + " " + entry.getLabel());
        badge.setStyle("-fx-font-size: 9px; -fx-text-fill: " + color + "; " +
                "-fx-background-color: " + color + "22; -fx-background-radius: 3; -fx-padding: 1 4;");

        String timeStr = formatTime(entry.start) + " – " + formatTime(entry.end);
        Label timeLbl = new Label(timeStr);
        timeLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        Label titleLbl = new Label(entry.title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleLbl.setWrapText(true);

        box.getChildren().addAll(timeLbl, titleLbl);
        if (inTimeGrid && entry.subtitle != null && !entry.subtitle.isEmpty()) {
            Label sub = new Label("👤 " + entry.subtitle);
            sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569;");
            box.getChildren().add(sub);
        }
        box.getChildren().add(badge);

        // Single click → quick popup; double click → edit form
        box.setOnMouseClicked(e -> {
            e.consume();
            if (e.getClickCount() == 2) {
                dismissPopup();
                openForm(entry.sourceReunion, null);
            } else {
                showQuickPopup(entry, box);
            }
        });
        box.setOnMouseEntered(e -> box.setStyle(eventBlockStyle(color, true)));
        box.setOnMouseExited(e -> box.setStyle(eventBlockStyle(color, false)));
        return box;
    }

    private String eventBlockStyle(String color, boolean hover) {
        String alpha = hover ? "55" : "33";
        return "-fx-background-color: " + color + alpha + "; " +
                "-fx-border-color: " + color + "; -fx-border-width: 0 0 0 3; " +
                "-fx-background-radius: 4; -fx-cursor: hand;";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Quick-peek popup (CalendarFX-inspired)
    // ═════════════════════════════════════════════════════════════════════

    private void showQuickPopup(CalendarEntry entry, Node anchor) {
        dismissPopup();
        if (calendarContainer.getScene() == null)
            return;

        String color = entry.getColor();

        VBox popup = new VBox(6);
        popup.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: " + color + "; -fx-border-radius: 10; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 16, 0, 0, 4); " +
                "-fx-padding: 12 14 12 14;");
        popup.setMaxWidth(260);
        popup.setPrefWidth(260);

        // Header with type badge
        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(entry.getIcon());
        icon.setStyle("-fx-font-size: 18px;");
        Label typeBadge = new Label(entry.getLabel());
        typeBadge.setStyle("-fx-background-color: " + color + "22; " +
                "-fx-text-fill: " + color + "; -fx-font-size: 10px; " +
                "-fx-background-radius: 4; -fx-padding: 2 6; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
        head.getChildren().addAll(icon, typeBadge, sp, closeBtn);
        popup.getChildren().add(head);

        // Title
        Label titleLbl = new Label(entry.title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleLbl.setWrapText(true);
        popup.getChildren().add(titleLbl);

        // Time
        if (entry.start != null) {
            Label timeLbl = new Label("🕐  " + formatDate(entry.start) + "  ·  "
                    + formatTime(entry.start) + " – " + formatTime(entry.end));
            timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            popup.getChildren().add(timeLbl);
        }

        // Organiser
        if (entry.subtitle != null && !entry.subtitle.isEmpty()) {
            Label orgLbl = new Label("👤  " + entry.subtitle);
            orgLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            popup.getChildren().add(orgLbl);
        }

        // Cancelled banner
        if (entry.cancelled) {
            Label cLbl = new Label("⚠  Annulé");
            cLbl.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                    "-fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px;");
            popup.getChildren().add(cLbl);
        }

        // Action buttons row (Edit + Delete)
        if (entry.sourceReunion != null) {
            Button editBtn = new Button("\u270F  Modifier");
            editBtn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-background-radius: 6; " +
                    "-fx-padding: 5 12; -fx-cursor: hand;");
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setMaxWidth(Double.MAX_VALUE);
            editBtn.setOnAction(e -> {
                dismissPopup();
                openForm(entry.sourceReunion, null);
            });

            Button deleteBtn = new Button("\uD83D\uDDD1  Supprimer");
            deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                    "-fx-font-size: 11px; -fx-background-radius: 6; " +
                    "-fx-border-color: #fca5a5; -fx-border-radius: 6; " +
                    "-fx-padding: 5 12; -fx-cursor: hand;");
            HBox.setHgrow(deleteBtn, Priority.ALWAYS);
            deleteBtn.setMaxWidth(Double.MAX_VALUE);
            deleteBtn.setOnAction(e -> {
                dismissPopup();
                handleDeleteReunion(entry.sourceReunion);
            });

            HBox btnRow = new HBox(6, editBtn, deleteBtn);
            btnRow.setAlignment(Pos.CENTER);
            popup.getChildren().add(btnRow);
        }

        // Fade in
        popup.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(150), popup);
        ft.setToValue(1);
        ft.play();

        closeBtn.setOnAction(e -> dismissPopup());

        // Use a JavaFX Popup so the scene root is never touched
        Popup popupWindow = new Popup();
        popupWindow.setAutoHide(true);
        popupWindow.setAutoFix(true);
        popupWindow.getContent().add(popup);

        // Position the popup near the anchor node
        javafx.geometry.Bounds boundsInScreen = anchor.localToScreen(anchor.getBoundsInLocal());
        if (boundsInScreen != null) {
            popupWindow.show(calendarContainer.getScene().getWindow(),
                    boundsInScreen.getMaxX() + 8,
                    boundsInScreen.getMinY());
        } else {
            popupWindow.show(calendarContainer.getScene().getWindow());
        }

        activePopup = popupWindow;
    }

    private void dismissPopup() {
        if (activePopup == null)
            return;
        activePopup.hide();
        activePopup = null;
    }

    // ═════════════════════════════════════════════════════════════════════
    // CRUD form (ouvre ajouterReu.fxml en modal)
    // ═════════════════════════════════════════════════════════════════════

    private void handleDeleteReunion(Reunion reunion) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la r\u00e9union");
        confirm.setHeaderText("Supprimer \u00ab\u00a0" + reunion.getTitre() + "\u00a0\u00bb ?");
        confirm.setContentText("Cette action est irr\u00e9versible. Voulez-vous continuer ?");

        // Style the confirm button red
        ButtonType btnDelete = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnDelete, btnCancel);

        confirm.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                try {
                    serviceReunion.delete(reunion);
                    refreshData();
                    renderAll();
                } catch (Exception ex) {
                    showAlert("Erreur", "Impossible de supprimer la r\u00e9union : " + ex.getMessage());
                }
            }
        });
    }

    private void openForm(Reunion existing, LocalDateTime defaultDT) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/planification/ajouterReu.fxml"));
            VBox root = loader.load();
            AjouterReuCont controller = loader.getController();

            if (existing != null) {
                controller.setReunion(existing);
            } else if (defaultDT != null) {
                controller.prefillDateTime(defaultDT);
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initOwner(calendarContainer.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setTitle(existing == null ? "Nouvelle réunion" : "Modifier la réunion");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            // Après fermeture du formulaire : recharger depuis la BD et rafraîchir le calendrier
            refreshData();
            renderAll();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de réunion : " + e.getMessage());
        }
    }

    private String typeToDescPrefix(String typeLabel) {
        switch (typeLabel) {
            case "Événement":
                return "[EVENT] ";
            case "Onboarding":
                return "[ONBOARDING] ";
            case "Offboarding":
                return "[OFFBOARDING] ";
            case "Réservation":
                return "[RESERVATION] ";
            default:
                return "";
        }
    }

    private void styleTypeCombo(ComboBox<String> cb) {
        Map<String, String> colors = new LinkedHashMap<>();
        colors.put("Réunion", "#ef4444");
        colors.put("Événement", "#8b5cf6");
        colors.put("Réservation", "#3b82f6");
        colors.put("Onboarding", "#22c55e");
        colors.put("Offboarding", "#f97316");
        String col = colors.getOrDefault(cb.getValue(), "#667eea");
        cb.setStyle("-fx-background-color: " + col + "22; " +
                "-fx-border-color: " + col + "; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-font-size: 13px; -fx-text-fill: " + col + ";");
    }

    private void closeOverlay(StackPane overlay) {
        // No longer swaps scene root – overlays are now shown via Popup
        dismissPopup();
    }

    // ═════════════════════════════════════════════════════════════════════
    // View button styling
    // ═════════════════════════════════════════════════════════════════════

    private void styleViewButtons() {
        String base = "-fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;";
        String active = base + " -fx-background-color: #667eea; -fx-text-fill: white;";
        String inactive = base + " -fx-background-color: transparent; -fx-text-fill: #475569;";

        dayBtn.setStyle((currentView == ViewMode.DAY ? active : inactive) + " -fx-background-radius: 7 0 0 7;");
        weekBtn.setStyle((currentView == ViewMode.WEEK ? active : inactive) + " -fx-background-radius: 0;");
        monthBtn.setStyle((currentView == ViewMode.MONTH ? active : inactive) + " -fx-background-radius: 0;");
        yearBtn.setStyle((currentView == ViewMode.YEAR ? active : inactive) + " -fx-background-radius: 0 7 7 0;");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Shared rendering helpers
    // ═════════════════════════════════════════════════════════════════════

    private Pane buildTimeColumn(double totalH) {
        Pane col = new Pane();
        col.setPrefWidth(58);
        col.setMinHeight(totalH);
        col.setStyle("-fx-background-color: white;");
        for (int h = HOUR_START; h <= HOUR_END; h++) {
            double y = (h - HOUR_START) * HOUR_HEIGHT;
            Label lbl = new Label(String.format("%02d:00", h));
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            lbl.setLayoutX(4);
            lbl.setLayoutY(y - 8);
            col.getChildren().add(lbl);

            Line sep = new Line(0, y, 58, y);
            sep.setStroke(Color.web("#f1f5f9"));
            sep.setStrokeWidth(1);
            col.getChildren().add(sep);
        }
        return col;
    }

    private void drawHourLines(Pane pane, double totalH) {
        for (int h = HOUR_START; h <= HOUR_END; h++) {
            double y = (h - HOUR_START) * HOUR_HEIGHT;
            Line line = new Line(0, y, 3000, y);
            line.setStroke(Color.web(h % 2 == 0 ? "#f1f5f9" : "#f8fafc"));
            line.setStrokeWidth(1);
            pane.getChildren().add(line);
        }
        // Half-hour dashes
        for (int h = HOUR_START; h < HOUR_END; h++) {
            double y = (h - HOUR_START + 0.5) * HOUR_HEIGHT;
            Line line = new Line(0, y, 3000, y);
            line.setStroke(Color.web("#f8fafc"));
            line.setStrokeWidth(0.5);
            line.getStrokeDashArray().addAll(4.0, 4.0);
            pane.getChildren().add(line);
        }
    }

    private void drawNowLine(Pane pane) {
        LocalTime now = LocalTime.now();
        double nowY = (now.getHour() - HOUR_START + now.getMinute() / 60.0) * HOUR_HEIGHT;
        if (nowY < 0 || nowY > (HOUR_END - HOUR_START) * HOUR_HEIGHT)
            return;

        Circle dot = new Circle(5);
        dot.setFill(Color.web("#667eea"));
        dot.setLayoutX(0);
        dot.setLayoutY(nowY);

        Line nowLine = new Line(0, nowY, 3000, nowY);
        nowLine.setStroke(Color.web("#667eea"));
        nowLine.setStrokeWidth(2);
        pane.getChildren().addAll(nowLine, dot);
    }

    private ScrollPane styledScroll(Node content) {
        ScrollPane sc = new ScrollPane(content);
        sc.setFitToWidth(true);
        sc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sc.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sc.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return sc;
    }

    private void scrollToHour(ScrollPane sc, double totalH, int hour) {
        double pos = ((hour - HOUR_START) * HOUR_HEIGHT) / totalH;
        sc.setVvalue(Math.max(0, Math.min(1, pos)));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Form field helpers
    // ═════════════════════════════════════════════════════════════════════

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; -fx-padding: 8 10;");
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        return l;
    }

    private VBox column(String label, Control field) {
        VBox col = new VBox(4);
        HBox.setHgrow(col, Priority.ALWAYS);
        col.getChildren().addAll(fieldLabel(label), field);
        field.setMaxWidth(Double.MAX_VALUE);
        return col;
    }

    private ComboBox<String> timeCombo() {
        ComboBox<String> cb = new ComboBox<>();
        for (int h = 0; h < 24; h++)
            for (int m = 0; m < 60; m += 15)
                cb.getItems().add(String.format("%02d:%02d", h, m));
        cb.setValue("09:00");
        cb.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px;");
        return cb;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Utility helpers
    // ═════════════════════════════════════════════════════════════════════

    private LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime toLocalTime(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    private Date toDate(LocalDate date, String timeStr) {
        String[] parts = (timeStr != null ? timeStr : "00:00").split(":");
        int h = Integer.parseInt(parts[0]);
        int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return Date.from(date.atTime(h, m).atZone(ZoneId.systemDefault()).toInstant());
    }

    private String formatTime(Date d) {
        if (d == null)
            return "--:--";
        LocalTime lt = toLocalTime(d);
        return String.format("%02d:%02d", lt.getHour(), lt.getMinute());
    }

    private String formatDate(Date d) {
        if (d == null)
            return "";
        LocalDate ld = toLocalDate(d);
        return ld.getDayOfMonth() + " " +
                capitalize(ld.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)));
    }

    private int snapMinute(int min) {
        return (min / 15) * 15;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}