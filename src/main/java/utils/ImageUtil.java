package utils;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Shared helper for profile picture loading and avatar building.
 *
 * Accepted path formats:
 *   file:///absolute/path/photo.png   (from FileChooser.toURI().toString())
 *   http(s)://...                     (remote URL)
 *   null / blank                      → initials fallback
 */
public class ImageUtil {

    /**
     * Tries to load an Image from pdpPath.
     * Returns null if the path is blank, malformed, or the image fails to load.
     */
    public static Image loadImage(String pdpPath) {
        if (pdpPath == null || pdpPath.isBlank()) return null;
        try {
            String uri = pdpPath.startsWith("file:") || pdpPath.startsWith("http")
                    ? pdpPath
                    : new java.io.File(pdpPath).toURI().toString();
            Image img = new Image(uri, true);
            // Give background load a moment; check for instant error flag
            if (img.isError()) return null;
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds a circular avatar StackPane.
     *
     * If pdpPath loads successfully → circular clipped photo.
     * Otherwise → colored circle with two-letter initials.
     *
     * @param pdpPath   utilisateur.getPdp()
     * @param prenom    first name
     * @param nom       last name
     * @param radius    circle radius in px  (e.g. 28 for cards, 18 for navbar)
     * @param bgHex     background color hex (e.g. "#ede9fe")
     * @param fgHex     text / border color  (e.g. "#7c3aed")
     */
    public static StackPane buildAvatar(String pdpPath, String prenom, String nom,
                                        double radius, String bgHex, String fgHex) {
        StackPane pane = new StackPane();
        Image img = loadImage(pdpPath);

        if (img != null) {
            // Photo — clip to circle
            ImageView iv = new ImageView(img);
            iv.setFitWidth(radius * 2);
            iv.setFitHeight(radius * 2);
            iv.setPreserveRatio(false);
            Circle clip = new Circle(radius, radius, radius);
            iv.setClip(clip);

            Circle border = new Circle(radius, Color.web(bgHex));
            pane.getChildren().addAll(border, iv);
        } else {
            // Initials fallback
            Circle circle = new Circle(radius, Color.web(bgHex));
            Label lbl = new Label(initials(prenom, nom));
            lbl.setStyle("-fx-text-fill: " + fgHex + "; -fx-font-size: "
                    + Math.max(10, (int)(radius * 0.58)) + "; -fx-font-weight: bold;");
            pane.getChildren().addAll(circle, lbl);
        }
        return pane;
    }

    /** 28px purple avatar — used in GestionUtilisateur & CandidatsAcceptes cards. */
    public static StackPane buildCardAvatar(String pdpPath, String prenom, String nom) {
        return buildAvatar(pdpPath, prenom, nom, 28, "#ede9fe", "#7c3aed");
    }

    /** 28px amber avatar — used in ArchiveUtilisateur cards. */
    public static StackPane buildArchiveAvatar(String pdpPath, String prenom, String nom) {
        return buildAvatar(pdpPath, prenom, nom, 28, "#fef3c7", "#d97706");
    }

    /** 18px navbar avatar — used in AdminDashboard top bar. */
    public static StackPane buildNavbarAvatar(String pdpPath, String prenom, String nom) {
        return buildAvatar(pdpPath, prenom, nom, 18, "#6366f1", "white");
    }

    private static String initials(String prenom, String nom) {
        String p = prenom != null ? prenom.trim() : "";
        String n = nom    != null ? nom.trim()    : "";
        if (p.isEmpty() && n.isEmpty()) return "?";
        char pc = p.isEmpty() ? '?' : p.charAt(0);
        char nc = n.isEmpty() ? '?' : n.charAt(0);
        return ("" + pc + nc).toUpperCase();
    }
}