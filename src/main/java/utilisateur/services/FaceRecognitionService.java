package utilisateur.services;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import utilisateur.models.Utilisateur;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionService {

    private static boolean opencvLoaded = false;
    private CascadeClassifier detecteurVisage;
    private VideoCapture camera;

    // Lower threshold = more permissive (0.55 works well for same-session capture)
    private static final double SEUIL_CORRESPONDANCE = 0.55;
    private static final int    TAILLE_VISAGE        = 200;

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init() {
        if (!opencvLoaded) {
            nu.pattern.OpenCV.loadLocally();
            opencvLoaded = true;
        }

        try {
            InputStream is = getClass()
                    .getResourceAsStream("/views/user/haarcascades/haarcascade_frontalface_alt.xml");
            if (is == null) throw new RuntimeException("Fichier Haar Cascade introuvable !");

            Path tmp = Files.createTempFile("haarcascade", ".xml");
            tmp.toFile().deleteOnExit();
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();

            detecteurVisage = new CascadeClassifier(tmp.toAbsolutePath().toString());
            if (detecteurVisage.empty())
                throw new RuntimeException("Impossible de charger le detecteur de visage.");

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement Haar Cascade", e);
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    public boolean startCamera() {
        camera = new VideoCapture(0);
        if (!camera.isOpened()) return false;
        // Warm up — discard the first few frames (camera needs time to adjust exposure)
        Mat warmup = new Mat();
        for (int i = 0; i < 5; i++) camera.read(warmup);
        return true;
    }

    public void stopCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
    }

    public Mat captureRawFrame() {
        if (camera == null || !camera.isOpened()) return null;
        Mat frame = new Mat();
        if (camera.read(frame) && !frame.empty()) return frame;
        return null;
    }

    // ── Face detection ────────────────────────────────────────────────────────

    public Mat detectAndExtractFace(Mat frame) {
        if (frame == null || frame.empty()) return null;

        Mat gray = toGray(frame);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        detecteurVisage.detectMultiScale(
                gray, faces,
                1.1,   // scaleFactor
                4,     // minNeighbors (lower = more detections, less strict)
                0,
                new Size(60, 60),  // minSize — smaller to catch faces further away
                new Size()
        );

        Rect[] array = faces.toArray();
        if (array.length == 0) return null;

        // Use the largest detected face
        Rect largest = array[0];
        for (Rect r : array) if (r.area() > largest.area()) largest = r;

        // Add 10% padding around the face for better histogram coverage
        int pad = (int)(largest.width * 0.10);
        int x = Math.max(0, largest.x - pad);
        int y = Math.max(0, largest.y - pad);
        int w = Math.min(gray.cols() - x, largest.width  + 2 * pad);
        int h = Math.min(gray.rows() - y, largest.height + 2 * pad);
        Rect paddedRect = new Rect(x, y, w, h);

        Mat face = new Mat(gray, paddedRect);
        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(TAILLE_VISAGE, TAILLE_VISAGE));
        return resized;
    }

    /**
     * Captures multiple samples and returns the average histogram bytes.
     * Much more robust than a single capture for enrollment.
     * @param sampleCount how many frames to average (5 recommended)
     */
    public byte[] captureAverageHistogram(int sampleCount) throws InterruptedException {
        List<Mat> histograms = new ArrayList<>();

        for (int i = 0; i < sampleCount * 3; i++) {  // try up to 3x to get enough samples
            Thread.sleep(80);
            Mat frame = captureRawFrame();
            if (frame == null) continue;
            Mat face = detectAndExtractFace(frame);
            if (face == null) continue;
            histograms.add(computeHistogram(face));
            if (histograms.size() >= sampleCount) break;
        }

        if (histograms.isEmpty()) return null;

        // Average all histograms
        Mat avg = new Mat(histograms.get(0).rows(), histograms.get(0).cols(),
                histograms.get(0).type(), Scalar.all(0));
        for (Mat h : histograms) Core.add(avg, h, avg);
        Core.divide(avg, Scalar.all(histograms.size()), avg);
        Core.normalize(avg, avg, 0, 1, Core.NORM_MINMAX);

        return serializeHistogram(avg);
    }

    // ── Histogram ─────────────────────────────────────────────────────────────

    public Mat computeHistogram(Mat face) {
        Mat hist = new Mat();
        Imgproc.calcHist(
                List.of(face), new MatOfInt(0), new Mat(),
                hist, new MatOfInt(256), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    public byte[] serializeHistogram(Mat hist) {
        int total = (int) hist.total();
        float[] data = new float[total];
        hist.get(0, 0, data);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(hist.rows());
            dos.writeInt(hist.cols());
            dos.writeInt(hist.type());
            for (float f : data) dos.writeFloat(f);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur serialisation histogramme", e);
        }
    }

    public Mat deserialiserHistogramme(byte[] donnees) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(donnees))) {
            int rows = dis.readInt();
            int cols = dis.readInt();
            int type = dis.readInt();
            float[] data = new float[(donnees.length - 12) / 4];
            for (int i = 0; i < data.length; i++) data[i] = dis.readFloat();
            Mat hist = new Mat(rows, cols, type);
            hist.put(0, 0, data);
            return hist;
        } catch (IOException e) {
            throw new RuntimeException("Erreur deserialisation histogramme", e);
        }
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    public double comparerVisages(byte[] d1, byte[] d2) {
        Mat h1 = deserialiserHistogramme(d1);
        Mat h2 = deserialiserHistogramme(d2);
        return Imgproc.compareHist(h1, h2, Imgproc.CV_COMP_CORREL);
    }

    public Utilisateur trouverUtilisateur(byte[] visageCapture, List<Utilisateur> utilisateurs) {
        Utilisateur meilleur = null;
        double meilleurScore = -1;

        for (Utilisateur u : utilisateurs) {
            if (u.getDonneesFaciales() == null) continue;
            double score = comparerVisages(visageCapture, u.getDonneesFaciales());
            System.out.printf("[Face] %s %s -> %.4f%n", u.getPrenom(), u.getNom(), score);
            if (score > meilleurScore) { meilleurScore = score; meilleur = u; }
        }

        if (meilleur != null && meilleurScore >= SEUIL_CORRESPONDANCE) {
            System.out.printf("[Face] Correspondance: %s %s (%.4f)%n",
                    meilleur.getPrenom(), meilleur.getNom(), meilleurScore);
            return meilleur;
        }
        System.out.printf("[Face] Aucun visage reconnu (meilleur score: %.4f, seuil: %.2f)%n",
                meilleurScore, SEUIL_CORRESPONDANCE);
        return null;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /**
     * Draws green rectangles around detected faces.
     * Also converts BGR → RGB so colors display correctly in JavaFX.
     */
    public Mat drawFaceRectangles(Mat frame) {
        if (frame == null || frame.empty()) return frame;

        Mat gray = toGray(frame);
        MatOfRect faces = new MatOfRect();
        detecteurVisage.detectMultiScale(gray, faces, 1.1, 4, 0,
                new Size(60, 60), new Size());

        // Draw rectangles on a copy
        Mat display = frame.clone();
        for (Rect r : faces.toArray()) {
            Imgproc.rectangle(display, r.tl(), r.br(), new Scalar(0, 255, 0), 2);

            // Label above the rectangle
            Imgproc.putText(display, "Visage detecte",
                    new Point(r.x, r.y - 8),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.55,
                    new Scalar(0, 255, 0), 2);
        }

        if (faces.toArray().length == 0) {
            Imgproc.putText(display, "Aucun visage detecte - ajustez votre position",
                    new Point(10, 25),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(0, 0, 255), 2);
        }

        return display;
    }

    /**
     * Converts an OpenCV Mat to a JavaFX Image.
     * Handles BGR->RGB conversion so colors are correct in the UI.
     */
    public Image matToImage(Mat mat) {
        if (mat == null || mat.empty()) return null;

        // Convert BGR to RGB for correct display in JavaFX
        Mat display = new Mat();
        if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, display, Imgproc.COLOR_BGR2RGB);
        } else {
            display = mat;
        }

        int type = display.channels() > 1
                ? BufferedImage.TYPE_3BYTE_BGR  // Despite the name, data is now RGB
                : BufferedImage.TYPE_BYTE_GRAY;

        byte[] buffer = new byte[display.channels() * display.cols() * display.rows()];
        display.get(0, 0, buffer);

        BufferedImage img = new BufferedImage(display.cols(), display.rows(), type);
        System.arraycopy(buffer, 0,
                ((DataBufferByte) img.getRaster().getDataBuffer()).getData(),
                0, buffer.length);

        return SwingFXUtils.toFXImage(img, null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Mat toGray(Mat frame) {
        Mat gray = new Mat();
        if (frame.channels() > 1) {
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = frame.clone();
        }
        return gray;
    }
}