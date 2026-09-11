package recrutement.services;

import recrutement.models.Document;
import utils.MyDataBase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDocument {

    private final Connection cnx;

    public ServiceDocument() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /** Copy file into uploads folder and insert metadata into DB. Returns saved Document. */
    public Document add(File srcFile) throws Exception {
        Path uploads = Path.of("uploaded_documents");
        if (!Files.exists(uploads)) Files.createDirectories(uploads);
        String destName = System.currentTimeMillis() + "_" + srcFile.getName();
        Path dest = uploads.resolve(destName);
        Files.copy(srcFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

        String sql = "INSERT INTO document(name, path, type, uploaded_at) VALUES (?,?,?,?)";
        try (PreparedStatement p = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, srcFile.getName());
            p.setString(2, dest.toString());
            p.setString(3, getTypeFromName(srcFile.getName()));
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            p.setTimestamp(4, ts);
            p.executeUpdate();
            ResultSet keys = p.getGeneratedKeys();
            Document d = new Document();
            if (keys != null && keys.next()) {
                d.setId(keys.getInt(1));
            }
            d.setName(srcFile.getName());
            d.setPath(dest.toString());
            d.setType(getTypeFromName(srcFile.getName()));
            d.setUploadedAt(ts);
            return d;
        }
    }

    public List<Document> getAll() {
        List<Document> out = new ArrayList<>();
        String sql = "SELECT id, name, path, type, uploaded_at FROM document ORDER BY uploaded_at DESC";
        try (Statement s = cnx.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                Document d = new Document();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setPath(rs.getString("path"));
                d.setType(rs.getString("type"));
                d.setUploadedAt(rs.getTimestamp("uploaded_at"));
                out.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    /** DELETE document from DB by ID. Also deletes the physical file. */
    public void delete(int id, String filePath) throws Exception {
        // 1. Delete from DB
        String sql = "DELETE FROM document WHERE id = ?";
        try (PreparedStatement p = cnx.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        }
        // 2. Delete physical file (best-effort — no error if already missing)
        try {
            if (filePath != null && !filePath.isBlank()) {
                Path path = Path.of(filePath);
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {}
    }

    private String getTypeFromName(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".pdf")) return "PDF";
        if (n.endsWith(".doc") || n.endsWith(".docx")) return "Doc";
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) return "Image";
        return "Fichier";
    }
}