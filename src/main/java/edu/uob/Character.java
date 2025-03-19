package edu.uob;

public class Character extends GameEntity {
    private Location currentLocation;

    public Character(String name, String description) {
        super(name, description);
    }

    public Location getCurrentLocation() {
        return this.currentLocation;
    }

    public void setCurrentLocation(Location location) {
        // Remove from old location if it exists
        if (this.currentLocation != null) {
            this.currentLocation.removeCharacter(this);
        }

        // Set new location and add character to it
        this.currentLocation = location;
        if (location != null) {
            location.addCharacter(this);
        }
    }
}