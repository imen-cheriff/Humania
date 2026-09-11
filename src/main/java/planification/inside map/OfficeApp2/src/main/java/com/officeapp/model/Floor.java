package com.officeapp.model;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * Immutable data model for a single floor.
 *
 * Stores the background image path, the elevator zone (as a rectangle on the
 * 900×600 scene), and the spawn point where the character appears after
 * travelling to this floor via the elevator.
 */
public class Floor {

    /** Human-readable label shown in the elevator popup buttons. */
    private final String displayName;

    /** Classpath resource path, e.g. "/images/floor1.png". */
    private final String imagePath;

    /**
     * Rectangular area that represents the elevator door / shaft on this floor.
     * Expressed in scene coordinates (0,0 = top-left).
     * When the character's bounding box intersects this rectangle, the elevator
     * popup is shown.
     */
    private final Rectangle2D elevatorZone;

    /**
     * Where the character should be placed (centre of character) when arriving
     * on this floor via the elevator.
     */
    private final Point2D spawnPoint;

    // ── Constructor ──────────────────────────────────────────────────────────

    public Floor(String displayName,
                 String imagePath,
                 Rectangle2D elevatorZone,
                 Point2D spawnPoint) {
        this.displayName  = displayName;
        this.imagePath    = imagePath;
        this.elevatorZone = elevatorZone;
        this.spawnPoint   = spawnPoint;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String       getDisplayName()  { return displayName;  }
    public String       getImagePath()    { return imagePath;    }
    public Rectangle2D  getElevatorZone() { return elevatorZone; }
    public Point2D      getSpawnPoint()   { return spawnPoint;   }
}
