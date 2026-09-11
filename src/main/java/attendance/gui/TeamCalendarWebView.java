package attendance.gui;

import attendance.models.*;
import attendance.services.*;
import attendance.interfaces.service;
import javafx.concurrent.Worker;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class TeamCalendarWebView extends VBox {

    private final service<Absence>     serviceAbsence     = new ServiceAbsence();
    private final service<Conge>       serviceConge       = new ServiceConge();
    private final service<Utilisateur> serviceUtilisateur = new ServiceUtilisateur();

    private WebView    webView;
    private WebEngine  webEngine;

    private static final String[] COLORS = {
            "#6366f1","#f59e0b","#10b981","#ef4444","#3b82f6",
            "#8b5cf6","#ec4899","#06b6d4","#84cc16","#f97316",
            "#14b8a6","#a855f7","#0ea5e9","#22c55e","#eab308"
    };

    public TeamCalendarWebView() {
        initialize();
    }

    private void initialize() {
        this.setPrefHeight(750);
        this.setMaxHeight(750);
        this.setMinHeight(750);
        this.setStyle("-fx-background-color: transparent;");

        webView = new WebView();
        webView.setPrefHeight(750);
        webView.setMaxHeight(750);
        webView.setMinHeight(750);
        webView.setContextMenuEnabled(false);

        webEngine = webView.getEngine();
        webEngine.loadContent(generateHTML());
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) injectEvents();
        });

        this.getChildren().add(webView);
    }

    private String generateHTML() {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8"/>
<link href="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.css" rel="stylesheet"/>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    font-family: 'Segoe UI', system-ui, sans-serif;
    background: #f8faff;
    height: 750px;
    overflow: hidden;
  }

  /* LÉGENDE */
  #legend {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    padding: 10px 20px;
    background: white;
    border-bottom: 2px solid #f1f5f9;
  }
  .leg-label {
    font-size: 10px; font-weight: 700;
    color: #94a3b8; text-transform: uppercase;
    letter-spacing: 0.8px; margin-right: 4px;
  }
  .pill {
    display: inline-flex; align-items: center; gap: 6px;
    padding: 4px 12px 4px 8px;
    border-radius: 20px; font-size: 11px; font-weight: 600;
    color: white;
    box-shadow: 0 2px 8px rgba(0,0,0,0.12);
    transition: transform 0.15s, box-shadow 0.15s;
    cursor: default;
  }
  .pill:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.18); }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.6); flex-shrink: 0; }

  /* CALENDRIER */
  #calendar { padding: 16px 20px 12px; background: #f8faff; height: 706px; }

  .fc { font-family: 'Segoe UI', system-ui, sans-serif !important; }

  /* Toolbar */
  .fc .fc-toolbar { margin-bottom: 14px !important; }
  .fc .fc-toolbar-title {
    font-size: 18px !important; font-weight: 800 !important;
    color: #1e293b !important; letter-spacing: -0.5px;
  }
  .fc .fc-button {
    background: white !important;
    border: 1.5px solid #e2e8f0 !important;
    border-radius: 8px !important;
    font-size: 12px !important; font-weight: 600 !important;
    color: #475569 !important; padding: 6px 14px !important;
    text-transform: capitalize !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06) !important;
    transition: all 0.15s !important;
  }
  .fc .fc-button:hover {
    background: #667eea !important; border-color: #667eea !important;
    color: white !important; box-shadow: 0 4px 12px rgba(102,126,234,0.3) !important;
    transform: translateY(-1px) !important;
  }
  .fc .fc-button-active {
    background: linear-gradient(135deg, #667eea, #764ba2) !important;
    border-color: transparent !important; color: white !important;
    box-shadow: 0 4px 12px rgba(102,126,234,0.35) !important;
  }
  .fc .fc-today-button {
    background: linear-gradient(135deg, #667eea, #764ba2) !important;
    border-color: transparent !important; color: white !important;
  }

  /* En-têtes jours */
  .fc .fc-col-header-cell {
    background: linear-gradient(135deg, #667eea08, #764ba208) !important;
    border-bottom: 2px solid #e2e8f0 !important;
    padding: 10px 0 !important;
  }
  .fc .fc-col-header-cell-cushion {
    font-size: 11px !important; font-weight: 800 !important;
    color: #667eea !important; text-transform: uppercase !important;
    letter-spacing: 1px !important; text-decoration: none !important;
  }

  /* Cellules */
  .fc .fc-daygrid-day {
    background: white !important;
    min-height: 90px !important;
    transition: background 0.12s;
    border: 1px solid #f1f5f9 !important;
  }
  .fc .fc-daygrid-day:hover { background: #fafbff !important; }

  .fc .fc-daygrid-day-number {
    font-size: 13px !important; font-weight: 700 !important;
    color: #64748b !important; padding: 8px 10px !important;
    text-decoration: none !important;
  }

  /* Aujourd'hui */
  .fc .fc-day-today {
    background: linear-gradient(135deg, #eef2ff, #f5f3ff) !important;
  }
  .fc .fc-day-today .fc-daygrid-day-number {
    background: linear-gradient(135deg, #667eea, #764ba2) !important;
    color: white !important; border-radius: 50% !important;
    width: 28px !important; height: 28px !important;
    display: flex !important; align-items: center !important;
    justify-content: center !important; font-size: 12px !important;
    margin: 4px !important;
    box-shadow: 0 3px 10px rgba(102,126,234,0.4) !important;
  }

  /* Weekend */
  .fc .fc-day-sat, .fc .fc-day-sun { background: #fafafa !important; }
  .fc .fc-day-sat .fc-daygrid-day-number,
  .fc .fc-day-sun .fc-daygrid-day-number { color: #94a3b8 !important; }

  /* Événements */
  .fc .fc-event {
    border: none !important;
    border-radius: 6px !important;
    font-size: 11px !important;
    font-weight: 600 !important;
    padding: 3px 7px !important;
    cursor: pointer !important;
    transition: filter 0.15s, transform 0.1s, box-shadow 0.15s !important;
    box-shadow: 0 1px 4px rgba(0,0,0,0.15) !important;
    letter-spacing: 0.1px;
  }
  .fc .fc-event:hover {
    filter: brightness(1.12) !important;
    transform: translateY(-1px) scaleY(1.04) !important;
    box-shadow: 0 4px 12px rgba(0,0,0,0.22) !important;
    z-index: 10 !important;
  }
  .fc .fc-event-title { color: white !important; font-size: 11px !important; }
  .fc .fc-more-link {
    font-size: 10px !important; font-weight: 700 !important;
    color: #667eea !important; background: #eef2ff !important;
    padding: 2px 6px !important; border-radius: 4px !important;
    text-decoration: none !important;
  }

  /* Grille */
  .fc .fc-scrollgrid { border-color: #e2e8f0 !important; border-radius: 12px; overflow: hidden; }
  .fc .fc-scrollgrid td, .fc .fc-scrollgrid th { border-color: #f1f5f9 !important; }

  /* Vue liste */
  .fc .fc-list { border-color: #e2e8f0 !important; border-radius: 12px; overflow: hidden; }
  .fc .fc-list-day-cushion { background: linear-gradient(135deg, #667eea10, #764ba210) !important; }
  .fc .fc-list-day-text, .fc .fc-list-day-side-text { color: #1e293b !important; font-weight: 700 !important; text-decoration: none !important; }
  .fc .fc-list-event:hover td { background: #fafbff !important; }
  .fc .fc-list-event-title a { color: #1e293b !important; text-decoration: none !important; font-weight: 500 !important; }
  .fc .fc-list-empty { color: #94a3b8 !important; }

  /* Tooltip */
  #tip {
    position: fixed; display: none;
    background: white;
    border: 1px solid #e2e8f0;
    color: #1e293b; padding: 14px 16px;
    border-radius: 14px; font-size: 12px;
    pointer-events: none; z-index: 9999;
    max-width: 260px;
    box-shadow: 0 16px 48px rgba(0,0,0,0.15), 0 4px 16px rgba(0,0,0,0.08);
    line-height: 1.6;
  }
  #tip .t-name { font-size: 14px; font-weight: 800; color: #1e293b; margin-bottom: 6px; letter-spacing: -0.2px; }
  #tip .t-badge {
    display: inline-flex; align-items: center; gap: 5px;
    padding: 3px 10px; border-radius: 20px;
    font-size: 11px; font-weight: 700; margin-bottom: 8px;
  }
  #tip .t-row { display: flex; align-items: center; gap: 7px; color: #64748b; margin-top: 3px; font-size: 12px; }
  #tip .t-row b { color: #1e293b; }
  #tip .t-divider { height: 1px; background: #f1f5f9; margin: 8px 0; }
</style>
</head>
<body>

<div id="legend">
  <span class="leg-label">Membres</span>
  <span id="legendContent" style="color:#94a3b8;font-size:11px;font-style:italic;">Chargement...</span>
</div>

<div id="calendar"></div>
<div id="tip"></div>

<script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.js"></script>
<script>
let cal;

document.addEventListener('DOMContentLoaded', function() {
  cal = new FullCalendar.Calendar(document.getElementById('calendar'), {
    initialView: 'dayGridMonth',
    locale: 'fr',
    height: 674,
    headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,listMonth' },
    buttonText: { today: "Aujourd'hui", month: 'Mois', list: 'Liste' },
    firstDay: 1,
    dayMaxEvents: 4,
    moreLinkContent: function(args) { return '+' + args.num + ' autres'; },
    events: [],
    eventClick: function(i) { showTip(i.jsEvent, i.event); },
    eventMouseLeave: hideTip
  });
  cal.render();
});

function addEvents(evJson, legJson) {
  if (!cal) return;
  const events = JSON.parse(evJson);
  const legend = JSON.parse(legJson);
  const el = document.getElementById('legendContent');

  if (Object.keys(legend).length === 0) {
    el.innerHTML = '<span style="color:#94a3b8;font-style:italic;">Aucun événement à afficher</span>';
  } else {
    el.innerHTML = Object.entries(legend).map(([n, c]) =>
      '<span class="pill" style="background:' + c + '">' +
        '<span class="dot"></span>' + n +
      '</span>'
    ).join('');
  }

  cal.removeAllEvents();
  cal.addEventSource(events);
}

function showTip(e, ev) {
  const p   = ev.extendedProps;
  const col = ev.backgroundColor || '#667eea';
  const s   = ev.start ? ev.start.toLocaleDateString('fr-FR') : '?';
  const endD = ev.end ? new Date(ev.end - 86400000) : ev.start;
  const en  = endD ? endD.toLocaleDateString('fr-FR') : s;
  const isC = p.type === 'conge';

  const statut = p.statut || '';
  const statutColor = statut === 'Approuve' || statut === 'Approuv\u00e9' ? '#10b981' : statut === 'Refus\u00e9' ? '#ef4444' : '#f59e0b';

  const t = document.getElementById('tip');
  t.innerHTML =
    '<div class="t-name">' + (ev.title || '') + '</div>' +
    '<span class="t-badge" style="background:' + col + '18;color:' + col + '">' +
      (isC ? '&#127958; Cong&eacute;' : '&#9203; Autorisation') +
    '</span>' +
    '<div class="t-divider"></div>' +
    '<div class="t-row">&#128197; Du <b>' + s + '</b> au <b>' + en + '</b></div>' +
    (statut ? '<div class="t-row">&#9679; Statut : <b style="color:' + statutColor + '">' + statut + '</b></div>' : '');

  t.style.display = 'block';
  t.style.left = Math.min(e.clientX + 14, window.innerWidth - 280) + 'px';
  t.style.top  = Math.min(e.clientY + 14, window.innerHeight - 200) + 'px';
  setTimeout(hideTip, 5000);
}

function hideTip() {
  document.getElementById('tip').style.display = 'none';
}
</script>
</body>
</html>
        """;
    }

    public void refresh() {
        if (webEngine != null) injectEvents();
    }

    private void injectEvents() {
        try {
            List<Absence>     absences     = serviceAbsence.getAll();
            List<Conge>       conges       = serviceConge.getAll();
            List<Utilisateur> utilisateurs = serviceUtilisateur.getAll();

            Map<Integer, String> userNames = new HashMap<>();
            for (Utilisateur u : utilisateurs) userNames.put(u.getId(), u.getNomComplet());

            Set<Integer> userIds = new LinkedHashSet<>();
            absences.forEach(a -> userIds.add(a.getUtilisateurId()));
            conges.forEach(c -> userIds.add(c.getUtilisateurId()));

            Map<Integer, String> idToColor = new LinkedHashMap<>();
            int ci = 0;
            for (int id : userIds) { idToColor.put(id, COLORS[ci % COLORS.length]); ci++; }

            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
            StringBuilder ev = new StringBuilder("[");
            StringBuilder lg = new StringBuilder("{");

            boolean first = true;
            for (Map.Entry<Integer, String> e : idToColor.entrySet()) {
                String nom = userNames.getOrDefault(e.getKey(), "Utilisateur #" + e.getKey());
                if (!first) lg.append(",");
                lg.append("\"").append(escJ(nom)).append("\":\"").append(e.getValue()).append("\"");
                first = false;
            }
            lg.append("}");

            // Congés
            for (Conge c : conges) {
                if (c.getDateDebut() == null || c.getDateFin() == null) continue;
                String nom = userNames.getOrDefault(c.getUtilisateurId(), "Utilisateur #" + c.getUtilisateurId());
                String col = idToColor.getOrDefault(c.getUtilisateurId(), "#6366f1");
                if (ev.length() > 1) ev.append(",");
                ev.append(String.format(
                        "{\"title\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"," +
                                "\"backgroundColor\":\"%s\",\"borderColor\":\"%s\"," +
                                "\"extendedProps\":{\"type\":\"conge\",\"statut\":\"%s\"}}",
                        escJ(nom + " - Conge"),
                        c.getDateDebut().format(fmt),
                        c.getDateFin().plusDays(1).format(fmt),
                        col, col,
                        escJ(c.getStatut() != null ? c.getStatut() : "")
                ));
            }

            // Absences — même couleur employé mais transparente
            for (Absence a : absences) {
                if (a.getDateDebut() == null) continue;
                String nom = userNames.getOrDefault(a.getUtilisateurId(), "Utilisateur #" + a.getUtilisateurId());
                String col = idToColor.getOrDefault(a.getUtilisateurId(), "#6366f1");
                String bg  = col + "88"; // semi-transparent
                if (ev.length() > 1) ev.append(",");
                ev.append(String.format(
                        "{\"title\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"," +
                                "\"backgroundColor\":\"%s\",\"borderColor\":\"%s\"," +
                                "\"extendedProps\":{\"type\":\"absence\",\"statut\":\"%s\"}}",
                        escJ(nom + " - Absence"),
                        a.getDateDebut().format(fmt),
                        a.getDateFin() != null ? a.getDateFin().plusDays(1).format(fmt) : a.getDateDebut().plusDays(1).format(fmt),
                        bg, col,
                        escJ(a.getStatut() != null ? a.getStatut() : "")
                ));
            }

            ev.append("]");
            String script = "addEvents('" +
                    ev.toString().replace("'", "\\'") + "','" +
                    lg.toString().replace("'", "\\'") + "')";
            webEngine.executeScript(script);

        } catch (Exception e) {
            System.err.println("Erreur TeamCalendarWebView: " + e.getMessage());
        }
    }

    private String escJ(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ");
    }
}