package competence.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

public class VlcVideoPlayer extends VBox {

    private MediaPlayerFactory  factory;
    private EmbeddedMediaPlayer mediaPlayer;
    private Canvas              canvas;
    private GraphicsContext     gc;
    private AnimationTimer      renderTimer;

    // Frame buffer thread-safe
    private final AtomicReference<int[]> frameBuffer = new AtomicReference<>();
    private volatile int     frameW = 1280;
    private volatile int     frameH = 720;
    private volatile boolean newFrameAvailable = false;

    // UI refs
    private Button   btnPlay;
    private Button   btnMute;
    private Slider   sliderProgress;
    private Slider   sliderVolume;
    private Label    lblTime;
    private Label    lblDuration;
    private VBox     controlsBox;        // barre de contrôles
    private StackPane videoPane;         // zone vidéo

    // État
    private boolean  isPlaying   = false;
    private boolean  isMuted     = false;
    private boolean  isDragging  = false;
    private boolean  isVideoFS   = false; // plein écran vidéo seulement
    private float    playbackRate = 1.0f;
    private String   currentQuality = "720p";

    // Auto-hide contrôles
    private Timeline hideTimer;
    private boolean  controlsVisible = true;

    // ──────────────────────────────────────────────────────────────────────
    public VlcVideoPlayer(String videoUrl, String title) {
        setStyle("-fx-background-color:#000000;");
        VBox.setVgrow(this, Priority.ALWAYS);

        if (!tryInitVlc()) { buildFallback(videoUrl); return; }

        buildUI();
        startRenderLoop();
        playUrl(videoUrl);
    }

    // ── Init VLC ──────────────────────────────────────────────────────────
    private boolean tryInitVlc() {
        try {
            factory = new MediaPlayerFactory("--intf=dummy","--no-video-title-show",
                    "--no-snapshot-preview","--quiet","--no-osd");
            mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
            System.out.println("[VLC] Factory ✓");
            return true;
        } catch (Throwable t) {
            System.err.println("[VLC] Init failed: " + t.getMessage());
            if (factory != null) { try { factory.release(); } catch (Exception ignored) {} factory = null; }
            return false;
        }
    }

    // ── Fallback ──────────────────────────────────────────────────────────
    private void buildFallback(String url) {
        setAlignment(Pos.CENTER); setSpacing(16); setPadding(new Insets(40));
        Label ico = new Label("🎬"); ico.setStyle("-fx-font-size:48;");
        Label msg = new Label("VLC non détecté — installez VLC 64-bit depuis videolan.org");
        msg.setStyle("-fx-text-fill:white;-fx-font-size:14;"); msg.setWrapText(true);
        Button btn = new Button("▶  Ouvrir dans le navigateur");
        btn.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:13;-fx-padding:10 24;-fx-background-radius:10;-fx-cursor:hand;");
        btn.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); } catch (Exception ex) { ex.printStackTrace(); } });
        getChildren().addAll(ico, msg, btn);
    }

    // ── Construction UI ───────────────────────────────────────────────────
    private void buildUI() {
        canvas = new Canvas(frameW, frameH);
        gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, frameW, frameH);

        controlsBox = buildControls();
        controlsBox.setMinHeight(78);
        controlsBox.setPrefHeight(78);
        controlsBox.setMaxHeight(78);

        // Zone vidéo avec overlay contrôles
        videoPane = new StackPane(canvas);
        videoPane.setStyle("-fx-background-color:black;");
        VBox.setVgrow(videoPane, Priority.ALWAYS);
        videoPane.setMinHeight(200);
        videoPane.widthProperty().addListener((o, ov, nv)  -> canvas.setWidth(nv.doubleValue()));
        videoPane.heightProperty().addListener((o, ov, nv) -> canvas.setHeight(nv.doubleValue()));

        // Double-clic = play/pause
        videoPane.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) togglePlay();
        });

        // Mouvement souris → montrer les contrôles + reset timer
        videoPane.addEventHandler(MouseEvent.MOUSE_MOVED, e -> showControls());
        videoPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> showControls());
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> showControls());

        setupHideTimer();

        getChildren().addAll(videoPane, controlsBox);
    }

    // ── Timer auto-hide 5 secondes ────────────────────────────────────────
    private void setupHideTimer() {
        hideTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (isPlaying) hideControls();
        }));
        hideTimer.setCycleCount(1);
    }

    private void showControls() {
        if (hideTimer == null) return; // pas encore initialisé ou déjà disposé
        if (!controlsVisible) {
            controlsVisible = true;
            controlsBox.setVisible(true);
            controlsBox.setManaged(true);
            setCursor(Cursor.DEFAULT);
            FadeTransition ft = new FadeTransition(Duration.millis(200), controlsBox);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
        hideTimer.stop();
        hideTimer.playFromStart();
    }

    private void hideControls() {
        if (hideTimer == null || !controlsVisible) return;
        controlsVisible = false;
        FadeTransition ft = new FadeTransition(Duration.millis(400), controlsBox);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            controlsBox.setVisible(false);
            controlsBox.setManaged(false);
            setCursor(Cursor.NONE);
        });
        ft.play();
    }

    // ── Barre de contrôles ────────────────────────────────────────────────
    private VBox buildControls() {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color:rgba(17,24,39,0.92);-fx-padding:8 14 10 14;");
        box.setFillWidth(true);

        // Slider progression
        sliderProgress = new Slider(0, 1, 0);
        sliderProgress.setMaxWidth(Double.MAX_VALUE);
        sliderProgress.setPrefHeight(16);
        sliderProgress.setStyle("-fx-accent:#4F46E5;");
        sliderProgress.setOnMousePressed(e  -> { isDragging = true;  showControls(); });
        sliderProgress.setOnMouseReleased(e -> {
            isDragging = false;
            if (mediaPlayer != null) mediaPlayer.controls().setPosition((float) sliderProgress.getValue());
        });
        sliderProgress.setOnMouseMoved(e -> showControls());

        // ── Ligne boutons ─────────────────────────────────────────────────
        btnPlay = mkBtn("▶  Play", "#4F46E5");
        btnPlay.setOnAction(e -> { togglePlay(); showControls(); });

        Button btnStop = mkBtn("⏹", "#374151");
        btnStop.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.controls().stop(); });

        Button btnBack = mkBtn("⏪ -10s", "#374151");
        btnBack.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.controls().skipTime(-10_000); showControls(); });

        Button btnFwd = mkBtn("+10s ⏩", "#374151");
        btnFwd.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.controls().skipTime(10_000); showControls(); });

        lblTime     = lbl("0:00");
        lblDuration = lbl("0:00");
        Label sep   = lbl(" / ");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Volume
        btnMute = mkBtn("🔊", "#374151");
        btnMute.setOnAction(e -> toggleMute());
        sliderVolume = new Slider(0, 200, 100);
        sliderVolume.setPrefWidth(90); sliderVolume.setMinWidth(70);
        sliderVolume.setStyle("-fx-accent:#10B981;");
        sliderVolume.valueProperty().addListener((o, ov, nv) -> {
            if (mediaPlayer != null) mediaPlayer.audio().setVolume(nv.intValue());
        });
        sliderVolume.setOnMouseMoved(e -> showControls());

        // ── Vitesse ───────────────────────────────────────────────────────
        Button btnSpeed = mkBtn("1x ⚡", "#374151");
        btnSpeed.setOnAction(e -> showSpeedMenu(btnSpeed));

        // ── Qualité ───────────────────────────────────────────────────────
        Button btnQuality = mkBtn("720p ⚙", "#374151");
        btnQuality.setOnAction(e -> showQualityMenu(btnQuality));

        // ── Plein écran vidéo seulement ───────────────────────────────────
        Button btnFS = mkBtn("⛶", "#374151");
        btnFS.setTooltip(new Tooltip("Plein écran vidéo"));
        btnFS.setOnAction(e -> toggleVideoFullscreen());

        HBox row = new HBox(7,
                btnPlay, btnStop, btnBack, btnFwd,
                lblTime, sep, lblDuration,
                spacer,
                btnMute, sliderVolume,
                btnSpeed, btnQuality, btnFS
        );
        row.setAlignment(Pos.CENTER_LEFT);

        // Garder la référence pour mettre à jour le label vitesse/qualité
        // On stocke les boutons via closure
        this.speedBtn   = btnSpeed;
        this.qualityBtn = btnQuality;

        box.getChildren().addAll(sliderProgress, row);
        return box;
    }

    // Références boutons pour mise à jour label
    private Button speedBtn;
    private Button qualityBtn;

    // ── Menu Vitesse ──────────────────────────────────────────────────────
    private void showSpeedMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        String[] labels = {"0.25x", "0.5x", "0.75x", "1x (Normal)", "1.25x", "1.5x", "1.75x", "2x"};
        for (int i = 0; i < speeds.length; i++) {
            final float sp = speeds[i]; final String lb = labels[i];
            MenuItem item = new MenuItem((sp == playbackRate ? "✓ " : "   ") + lb);
            item.setStyle("-fx-font-size:12;");
            item.setOnAction(ev -> {
                playbackRate = sp;
                if (mediaPlayer != null) mediaPlayer.controls().setRate(sp);
                speedBtn.setText((sp == 1.0f ? "1x" : lb.replace(" (Normal)","")) + " ⚡");
                showControls();
            });
            menu.getItems().add(item);
        }
        menu.show(anchor, javafx.geometry.Side.TOP, 0, 0);
    }

    // ── Menu Qualité ──────────────────────────────────────────────────────
    // Les qualités disponibles dépendent de ce que yt-dlp a résolu.
    // On stocke les URLs par qualité et on recharge si changement.
    private java.util.Map<String, String> qualityUrls = new java.util.HashMap<>();
    private String originalYtUrl = "";

    private void showQualityMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        String[] qualities = {"1080p", "720p", "480p", "360p"};
        for (String q : qualities) {
            MenuItem item = new MenuItem((q.equals(currentQuality) ? "✓ " : "   ") + q);
            item.setStyle("-fx-font-size:12;");
            item.setOnAction(ev -> {
                if (!q.equals(currentQuality)) {
                    currentQuality = q;
                    qualityBtn.setText(q + " ⚙");
                    changeQuality(q);
                }
                showControls();
            });
            menu.getItems().add(item);
        }
        menu.show(anchor, javafx.geometry.Side.TOP, 0, 0);
    }

    private void changeQuality(String quality) {
        if (originalYtUrl.isBlank()) return;
        // Sauvegarder position actuelle
        final float pos = (mediaPlayer != null) ? mediaPlayer.status().position() : 0f;
        new Thread(() -> {
            String height = quality.replace("p","");
            String url = resolveYtDlp(originalYtUrl, height);
            Platform.runLater(() -> {
                if (mediaPlayer != null) {
                    mediaPlayer.media().play(url);
                    // Restaurer position après quelques ms
                    new Timeline(new KeyFrame(Duration.millis(1500), e -> {
                        if (mediaPlayer != null) {
                            mediaPlayer.controls().setPosition(pos);
                            mediaPlayer.controls().setRate(playbackRate);
                        }
                    })).play();
                }
            });
        }, "vlc-quality-thread").start();
    }

    // ── Plein écran vidéo seulement (pas toute la fenêtre) ───────────────
    private Stage  fsStage;
    private Canvas fsCanvas;

    private void toggleVideoFullscreen() {
        if (!isVideoFS) {
            enterVideoFullscreen();
        } else {
            exitVideoFullscreen();
        }
    }

    private void enterVideoFullscreen() {
        isVideoFS = true;

        fsStage = new Stage();
        fsStage.setTitle("Vidéo — Plein écran");

        // Canvas vidéo qui remplit tout
        fsCanvas = new Canvas();
        StackPane fsVideoPane = new StackPane(fsCanvas);
        fsVideoPane.setStyle("-fx-background-color:black;");
        fsVideoPane.widthProperty().addListener((o, ov, nv)  -> fsCanvas.setWidth(nv.doubleValue()));
        fsVideoPane.heightProperty().addListener((o, ov, nv) -> fsCanvas.setHeight(nv.doubleValue()));

        // Barre de contrôles en bas, transparente, par-dessus la vidéo
        VBox fsControls = buildFsControls();
        fsControls.setMaxWidth(Double.MAX_VALUE);
        fsControls.setPickOnBounds(false);

        // Placer la barre EN BAS via StackPane alignment
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color:black;");
        root.getChildren().addAll(fsVideoPane, fsControls);
        StackPane.setAlignment(fsControls, Pos.BOTTOM_CENTER);

        // Auto-hide dans le fullscreen
        Timeline fsHide = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), fsControls);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(ev -> { fsControls.setVisible(false); root.setCursor(Cursor.NONE); });
            ft.play();
        }));
        fsHide.setCycleCount(1);

        root.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            fsControls.setVisible(true);
            fsControls.setOpacity(1.0);
            root.setCursor(Cursor.DEFAULT);
            fsHide.stop();
            fsHide.playFromStart();
        });

        // Démarrer le timer
        fsHide.playFromStart();

        javafx.scene.Scene fsScene = new javafx.scene.Scene(root, Color.BLACK);

        // Echap = quitter plein écran
        fsScene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) exitVideoFullscreen();
        });

        fsStage.setScene(fsScene);
        fsStage.setFullScreen(true);
        fsStage.setFullScreenExitHint("");
        fsStage.setOnCloseRequest(e -> exitVideoFullscreen());
        fsStage.show();

        // Switcher le rendu vers fsCanvas
        renderTimer.stop();
        renderTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (!newFrameAvailable) return;
                int[] px = frameBuffer.get();
                if (px == null) return;
                newFrameAvailable = false;
                int w = frameW, h = frameH;
                if (px.length < w * h) return;
                try {
                    WritableImage img = new WritableImage(w, h);
                    img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
                    double cw = fsCanvas.getWidth(), ch = fsCanvas.getHeight();
                    if (cw <= 0 || ch <= 0) return;
                    double scale = Math.min(cw / w, ch / h);
                    double dw = w * scale, dh = h * scale;
                    double dx = (cw - dw) / 2.0, dy = (ch - dh) / 2.0;
                    GraphicsContext fsgc = fsCanvas.getGraphicsContext2D();
                    fsgc.setFill(Color.BLACK);
                    fsgc.fillRect(0, 0, cw, ch);
                    fsgc.drawImage(img, dx, dy, dw, dh);
                } catch (Exception ignored) {}
            }
        };
        renderTimer.start();
    }

    private VBox buildFsControls() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:linear-gradient(from 0% 0% to 0% 100%, transparent, rgba(0,0,0,0.85));" +
                "-fx-padding:30 20 16 20;");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPickOnBounds(false);

        Slider fsProg = new Slider(0, 1, 0);
        fsProg.setMaxWidth(Double.MAX_VALUE);
        fsProg.setStyle("-fx-accent:#4F46E5;");
        fsProg.setOnMousePressed(e  -> isDragging = true);
        fsProg.setOnMouseReleased(e -> {
            isDragging = false;
            if (mediaPlayer != null) mediaPlayer.controls().setPosition((float) fsProg.getValue());
        });
        // Sync avec le slider principal
        sliderProgress.valueProperty().addListener((o, ov, nv) -> {
            if (!isDragging) fsProg.setValue(nv.doubleValue());
        });

        Button fsPlay = mkBtn("⏸", "#4F46E5");
        fsPlay.setOnAction(e -> togglePlay());
        // Sync état play/pause
        btnPlay.textProperty().addListener((o, ov, nv) ->
                fsPlay.setText(isPlaying ? "⏸" : "▶"));

        Button fsBack = mkBtn("⏪ -10s", "#374151");
        fsBack.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.controls().skipTime(-10_000); });

        Button fsFwd = mkBtn("+10s ⏩", "#374151");
        fsFwd.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.controls().skipTime(10_000); });

        Label fsTime = lbl("0:00 / 0:00");
        lblTime.textProperty().addListener((o, ov, nv) ->
                fsTime.setText(nv + " / " + lblDuration.getText()));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button fsExit = new Button("✕  Quitter plein écran");
        fsExit.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-padding:6 14;-fx-background-radius:6;-fx-cursor:hand;");
        fsExit.setOnAction(e -> exitVideoFullscreen());

        HBox row = new HBox(10, fsPlay, fsBack, fsFwd, fsTime, sp, fsExit);
        row.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(fsProg, row);
        return box;
    }

    private void exitVideoFullscreen() {
        isVideoFS = false;
        if (fsStage != null) { fsStage.close(); fsStage = null; }

        // Revenir au rendu sur le canvas principal
        renderTimer.stop();
        startRenderLoop();
        showControls();
    }

    // ── Surface VLCJ ─────────────────────────────────────────────────────
    private void attachSurface() {
        BufferFormatCallback fmtCb = new BufferFormatCallback() {
            @Override public BufferFormat getBufferFormat(int w, int h) {
                System.out.println("[VLC] Size: " + w + "x" + h);
                frameW = w; frameH = h;
                return new RV32BufferFormat(w, h);
            }
            @Override public void allocatedBuffers(ByteBuffer[] bufs) {}
        };

        RenderCallback renderCb = (mp, nativeBufs, fmt) -> {
            int w = fmt.getWidth(), h = fmt.getHeight();
            ByteBuffer src = nativeBufs[0]; src.rewind();
            int[] px = new int[w * h];
            for (int i = 0; i < px.length; i++) {
                int b = src.get()&0xFF, g = src.get()&0xFF, r = src.get()&0xFF, a = src.get()&0xFF;
                px[i] = (a<<24)|(r<<16)|(g<<8)|b;
            }
            src.rewind();
            frameBuffer.set(px);
            newFrameAvailable = true;
        };

        CallbackVideoSurface surface = factory.videoSurfaces().newVideoSurface(fmtCb, renderCb, true);
        mediaPlayer.videoSurface().set(surface);
    }

    // ── AnimationTimer ────────────────────────────────────────────────────
    private void startRenderLoop() {
        renderTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (!newFrameAvailable) return;
                int[] px = frameBuffer.get();
                if (px == null) return;
                newFrameAvailable = false;
                int w = frameW, h = frameH;
                if (px.length < w * h || w <= 0 || h <= 0) return;
                try {
                    WritableImage img = new WritableImage(w, h);
                    img.getPixelWriter().setPixels(0,0,w,h, PixelFormat.getIntArgbInstance(), px, 0, w);
                    double cw = canvas.getWidth(), ch = canvas.getHeight();
                    if (cw <= 0 || ch <= 0) return;
                    double scale = Math.min(cw/w, ch/h);
                    double dw = w*scale, dh = h*scale, dx = (cw-dw)/2.0, dy = (ch-dh)/2.0;
                    gc.setFill(Color.BLACK); gc.fillRect(0,0,cw,ch);
                    gc.drawImage(img, dx, dy, dw, dh);
                } catch (Exception ignored) {}
            }
        };
        renderTimer.start();
        System.out.println("[VLC] AnimationTimer ✓");
    }

    // ── Lecture ───────────────────────────────────────────────────────────
    private void playUrl(String videoUrl) {
        originalYtUrl = videoUrl;
        attachSurface();

        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override public void playing(MediaPlayer mp) {
                Platform.runLater(() -> {
                    if (btnPlay == null || hideTimer == null) return;
                    isPlaying = true;
                    btnPlay.setText("⏸  Pause");
                    btnPlay.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;" +
                            "-fx-font-size:12;-fx-padding:6 10;-fx-background-radius:6;-fx-cursor:hand;");
                    hideTimer.playFromStart();
                });
            }
            @Override public void paused(MediaPlayer mp) {
                Platform.runLater(() -> {
                    if (btnPlay == null) return;
                    isPlaying = false;
                    btnPlay.setText("▶  Play");
                    btnPlay.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                            "-fx-font-size:12;-fx-padding:6 10;-fx-background-radius:6;-fx-cursor:hand;");
                    showControls();
                    if (hideTimer != null) hideTimer.stop();
                });
            }
            @Override public void stopped(MediaPlayer mp) {
                Platform.runLater(() -> {
                    isPlaying = false;
                    if (btnPlay != null) btnPlay.setText("▶  Play");
                    if (sliderProgress != null) sliderProgress.setValue(0);
                    if (lblTime != null) lblTime.setText("0:00");
                    showControls(); // null-safe déjà
                });
            }
            @Override public void timeChanged(MediaPlayer mp, long t) {
                Platform.runLater(() -> {
                    long total = mp.status().length();
                    if (total > 0 && !isDragging) {
                        sliderProgress.setValue((double) t / total);
                        lblTime.setText(fmt(t));
                        lblDuration.setText(fmt(total));
                    }
                });
            }
            @Override public void error(MediaPlayer mp) {
                System.err.println("[VLC] ❌ Erreur");
                Platform.runLater(() -> lblTime.setText("❌ Erreur"));
            }
        });

        new Thread(() -> {
            String resolved = resolveYtDlp(videoUrl, "720");
            if (mediaPlayer == null) return; // disposed before thread ran
            System.out.println("[VLC] play → " + resolved.substring(0, Math.min(60, resolved.length())) + "...");
            boolean ok = mediaPlayer.media().play(resolved);
            System.out.println("[VLC] play retour: " + ok);
        }, "vlc-play-thread").start();
    }

    // ── yt-dlp ────────────────────────────────────────────────────────────
    private String resolveYtDlp(String url, String maxHeight) {
        if (url == null || url.isBlank()) return "";
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return url;
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-g",
                    "-f", "best[height<=" + maxHeight + "]/best",
                    "--no-playlist", url);
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            String out = new String(proc.getInputStream().readAllBytes()).trim();
            if (proc.waitFor() == 0 && !out.isBlank()) {
                String line = out.split("\n")[0].trim();
                System.out.println("[yt-dlp] ✓ " + maxHeight + "p → " + line.substring(0, Math.min(60, line.length())) + "...");
                return line;
            }
        } catch (Exception e) { System.err.println("[yt-dlp] " + e.getMessage()); }
        return url;
    }

    // ── Contrôles ─────────────────────────────────────────────────────────
    private void togglePlay() {
        if (mediaPlayer == null) return;
        if (isPlaying) mediaPlayer.controls().pause();
        else           mediaPlayer.controls().play();
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        isMuted = !isMuted;
        mediaPlayer.audio().setMute(isMuted);
        btnMute.setText(isMuted ? "🔇" : "🔊");
    }

    // ── stopAndClear (navigation rapide) ──────────────────────────────────
    public void stopAndClear() {
        if (hideTimer != null) hideTimer.stop();
        if (renderTimer != null) renderTimer.stop();
        frameBuffer.set(null);
        newFrameAvailable = false;
        if (gc != null && canvas != null) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        if (fsStage != null) { Platform.runLater(() -> { if(fsStage!=null) fsStage.close(); fsStage=null; }); }
        if (mediaPlayer != null) { try { mediaPlayer.controls().stop(); } catch (Exception ignored) {} }
    }

    // ── Dispose complet ───────────────────────────────────────────────────
    public void dispose() {
        System.out.println("[VLC] dispose()");
        try { prefWidthProperty().unbind();  } catch (Exception ignored) {}
        try { prefHeightProperty().unbind(); } catch (Exception ignored) {}
        if (hideTimer  != null) { hideTimer.stop();  hideTimer  = null; }
        if (renderTimer!= null) { renderTimer.stop(); renderTimer = null; }
        if (mediaPlayer!= null) {
            try { mediaPlayer.controls().stop(); Thread.sleep(200); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (factory != null) { try { factory.release(); } catch (Exception ignored) {} factory = null; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Button mkBtn(String t, String bg) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                "-fx-font-size:12;-fx-padding:6 10;-fx-background-radius:6;-fx-cursor:hand;");
        b.setMinWidth(36);
        return b;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:#D1D5DB;-fx-font-size:12;-fx-font-family:monospace;");
        return l;
    }

    private String fmt(long ms) {
        long s = ms/1000, m = s/60; s %= 60; long h = m/60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%d:%02d",m,s);
    }
}