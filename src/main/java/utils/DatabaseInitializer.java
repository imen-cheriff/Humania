package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to initialize the database with required tables and test data
 */
public class DatabaseInitializer {

    private static final String SQL_SCRIPT = """
            -- ====================================================
            -- Base de Données: HR Management System
            -- ====================================================
            
            CREATE DATABASE IF NOT EXISTS humania_full;
            USE humania_full;
            
            -- TABLE: poste_externe
            CREATE TABLE IF NOT EXISTS poste_externe (
                id INT PRIMARY KEY AUTO_INCREMENT,
                titre VARCHAR(255) NOT NULL,
                description LONGTEXT,
                type_contrat VARCHAR(100),
                salaire DOUBLE,
                competences_requises LONGTEXT,
                experience_requise INT DEFAULT 0,
                niveau_etude_requis VARCHAR(100),
                statut VARCHAR(50) DEFAULT 'Disponible',
                date_publication DATE,
                date_cloture DATE,
                public_externe BOOLEAN DEFAULT TRUE,
                responsable_rh_id INT DEFAULT 1,
                nombre_employe INT DEFAULT 1,
                priorite INT DEFAULT 1
            );
            
            -- TABLE: candidature_externe
            CREATE TABLE IF NOT EXISTS candidature_externe (
                id INT PRIMARY KEY AUTO_INCREMENT,
                poste_externe_id INT,
                nom VARCHAR(100) NOT NULL,
                prenom VARCHAR(100) NOT NULL,
                date_depot DATE,
                statut VARCHAR(50) DEFAULT 'Nouvelles',
                etape_pipeline VARCHAR(100),
                scoring_ia DOUBLE DEFAULT 0.0,
                cv_url VARCHAR(500),
                lettre_motivation_url VARCHAR(500),
                derniere_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (poste_externe_id) REFERENCES poste_externe(id)
            );
            
            -- TABLE: poste_interne
            CREATE TABLE IF NOT EXISTS poste_interne (
                id INT PRIMARY KEY AUTO_INCREMENT,
                type_poste VARCHAR(100),
                remuneration DOUBLE DEFAULT 0.0,
                date_debut DATE,
                date_fin DATE
            );
            
            -- TABLE: candidature_interne
            CREATE TABLE IF NOT EXISTS candidature_interne (
                id INT PRIMARY KEY AUTO_INCREMENT,
                poste_actuel VARCHAR(255),
                nouveau_poste VARCHAR(255),
                nouveau_salaire DOUBLE DEFAULT 0.0,
                date_demande DATE,
                motif LONGTEXT,
                derniere_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            
            -- TABLE: entretien_recrutement
            CREATE TABLE IF NOT EXISTS entretien_recrutement (
                id INT PRIMARY KEY AUTO_INCREMENT,
                candidature_id INT,
                date_entretien DATE,
                type_entretien VARCHAR(100),
                notes LONGTEXT,
                resultat VARCHAR(50)
            );
            
            -- TABLE: evaluation_candidat
            CREATE TABLE IF NOT EXISTS evaluation_candidat (
                id INT PRIMARY KEY AUTO_INCREMENT,
                candidature_id INT,
                competences_score DOUBLE,
                experience_score DOUBLE,
                motivation_score DOUBLE,
                score_general DOUBLE,
                date_evaluation DATE
            );
            
            -- TABLE: matching_ai
            CREATE TABLE IF NOT EXISTS matching_ai (
                id INT PRIMARY KEY AUTO_INCREMENT,
                candidature_id INT,
                poste_id INT,
                score_matching DOUBLE,
                date_matching DATE
            );
            
            -- TABLE: pipeline_etape
            CREATE TABLE IF NOT EXISTS pipeline_etape (
                id INT PRIMARY KEY AUTO_INCREMENT,
                candidature_id INT,
                etape VARCHAR(100),
                date_etape DATE,
                notes LONGTEXT
            );
            """;

    /**
     * Initialize the database with tables and test data
     */
    public static void initialize() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            if (cnx != null) {
                System.out.println("🔧 Initializing database...");
                executeSqlStatements(cnx);
                insertTestData(cnx);
                System.out.println("✓ Database initialized successfully!");
            } else {
                System.err.println("✗ Failed to get database connection");
            }
        } catch (Exception e) {
            System.err.println("✗ Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSqlStatements(Connection cnx) throws SQLException {
        String[] statements = SQL_SCRIPT.split(";");
        try (Statement stmt = cnx.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static void insertTestData(Connection cnx) throws SQLException {
        String testDataSql = """
                -- Insert test data for poste_interne
                INSERT IGNORE INTO poste_interne (id, type_poste, remuneration, date_debut, date_fin)
                VALUES 
                    (1, 'Management', 3500, '2026-01-10', '2026-06-30'),
                    (2, 'Coordination', 2800, '2026-01-25', '2026-07-31'),
                    (3, 'Technique', 3200, '2026-02-01', '2026-08-31');
                
                -- Insert test data for candidature_interne
                INSERT IGNORE INTO candidature_interne (id, poste_actuel, nouveau_poste, nouveau_salaire, date_demande, motif)
                VALUES 
                    (1, 'Developer Senior', 'Engineering Lead', 4200, '2026-01-12', 'Ready for leadership role'),
                    (2, 'Developer', 'Engineering Lead', 4000, '2026-01-18', 'Need more experience'),
                    (3, 'Projecteur', 'Coordinateur', 3200, '2026-01-25', 'Strategic move'),
                    (4, 'Support IT', 'Technique Avancé', 3500, '2026-02-01', 'Career change');
                
                -- Insert test data for poste_externe
                INSERT IGNORE INTO poste_externe (id, titre, description, type_contrat, salaire, competences_requises, experience_requise, niveau_etude_requis, statut, date_publication)
                VALUES 
                    (1, 'Développeur Full Stack', 'Développement d\\'applications web modernes avec React et Spring Boot', 'CDI', 35000, 'React, Spring Boot, SQL, Git', 3, 'Bac+3', 'Ouvert', '2026-01-15'),
                    (2, 'Chef de Projet Digital', 'Pilotage de projets digitaux innovants', 'CDI', 45000, 'Gestion de projet, Agile, Leadership', 5, 'Bac+5', 'Ouvert', '2026-01-10'),
                    (3, 'Designer UX/UI', 'Conception d\\'interfaces utilisateur modernes', 'CDI', 32000, 'Figma, User Research, Prototyping', 2, 'Bac+3', 'Ouvert', '2026-01-20');
                
                -- Insert test data for candidature_externe
                INSERT IGNORE INTO candidature_externe (id, poste_externe_id, nom, prenom, date_depot, statut, etape_pipeline, scoring_ia)
                VALUES 
                    (1, 1, 'Dupont', 'Marie', '2026-02-10', 'En cours', 'Entretien', 0.85),
                    (2, 1, 'Martin', 'Lucas', '2026-02-12', 'En cours', 'Offre', 0.92),
                    (3, 2, 'Bernard', 'Sophie', '2026-02-08', 'Refusée', 'Test Technique', 0.65),
                    (4, 3, 'Petit', 'Emma', '2026-02-15', 'En attente', 'Réception', 0.75);
                """;

        String[] statements = testDataSql.split(";");
        try (Statement stmt = cnx.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }
}
