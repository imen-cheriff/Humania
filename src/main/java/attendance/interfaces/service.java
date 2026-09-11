package attendance.interfaces;

import java.util.List;


public interface service<Entity> {


    void add(Entity entity);


    List<Entity> getAll();


    void update(Entity entity);



    void delete(Entity entity);

    // ========================================
    // MÉTHODES SUPPLÉMENTAIRES (OPTIONNELLES)
    // ========================================
    // Vous pouvez décommenter et ajouter ces méthodes selon vos besoins

    // Récupérer une entité par son ID
    Entity getById(int id);  // ✅ DÉCOMMENTÉE ET RENOMMÉE
    // Récupérer des entités selon un statut
    // List<Entity> getByStatut(String statut);



    // Vérifier si une entité existe
    // boolean exists(int id);
}