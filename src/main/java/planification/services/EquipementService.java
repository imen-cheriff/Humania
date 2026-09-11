package planification.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EquipementService {

    private static final List<String> EQUIPEMENTS_DISPONIBLES = new ArrayList<>(Arrays.asList(
            "Projecteur",
            "Écran de projection",
            "Tableau blanc",
            "Tableau interactif",
            "Système de son",
            "Microphone",
            "Caméra vidéo",
            "WiFi",
            "Prise électrique",
            "Climatisation",
            "Éclairage LED",
            "Tables modulaires",
            "Chaises pliantes",
            "Estrade",
            "Système de visioconférence"
    ));

    /**
     * Retourne la liste de tous les équipements disponibles
     */
    public static List<String> getEquipementsDisponibles() {
        return new ArrayList<>(EQUIPEMENTS_DISPONIBLES);
    }

    /**
     * Affiche la liste des équipements disponibles avec leurs numéros
     */
    public static void afficherEquipementsDisponibles() {
        System.out.println("\n=== Équipements disponibles ===");
        for (int i = 0; i < EQUIPEMENTS_DISPONIBLES.size(); i++) {
            System.out.println((i + 1) + ". " + EQUIPEMENTS_DISPONIBLES.get(i));
        }
        System.out.println("0. Terminer la sélection");
    }

    /**
     * Retourne l'équipement à l'index donné
     */
    public static String getEquipement(int index) {
        if (index >= 1 && index <= EQUIPEMENTS_DISPONIBLES.size()) {
            return EQUIPEMENTS_DISPONIBLES.get(index - 1);
        }
        return null;
    }

    /**
     * Vérifie si un équipement existe
     */
    public static boolean existeEquipement(int index) {
        return index >= 1 && index <= EQUIPEMENTS_DISPONIBLES.size();
    }
}
