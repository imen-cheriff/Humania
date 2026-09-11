package utilisateur.test;

import javafx.application.Application;
import test.MainFX;

/**
 * Point d'entrée unique de l'application Humania.
 * ServiceAdmin est initialisé dans MainFX.start() avant l'affichage du login.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainFX.class, args);
    }
}