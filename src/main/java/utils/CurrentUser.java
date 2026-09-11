package utils;

import planification.models.UserModel;

/**
 * Simple helper representing the currently logged-in user.
 *
 * Replace this implementation with your real authentication/session system.
 */
public final class CurrentUser {

    private static UserModel currentUser;

    private CurrentUser() {
    }

    public static void set(UserModel user) {
        currentUser = user;
    }

    public static UserModel get() {
        if (currentUser == null) {
            // Fallback for development: a dummy non-admin user with id=1
            currentUser = new UserModel(1, "demo", "Demo User", false);
        }
        return currentUser;
    }

    public static int getId() {
        return get().getId();
    }

    public static boolean isAdmin() {
        return get().isAdmin();
    }
}

