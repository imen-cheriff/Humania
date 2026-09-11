package utilisateur.models;

import utilisateur.enums.Role;

public class Admin extends Utilisateur {

    public Admin() {
        super();
        this.role = Role.ADMIN;
    }


}