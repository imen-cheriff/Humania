package planification.models;

/**
 * Lightweight view model wrapper around an {@link Espace} entity
 * used specifically for coworking seat reservation screens.
 */
public class EspaceModel {

    private final int id;
    private final String nom;
    private final int capacite;

    public EspaceModel(Espace espace) {
        this.id = espace.getId();
        this.nom = espace.getNom();
        this.capacite = espace.getCapacite();
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getCapacite() {
        return capacite;
    }
}

