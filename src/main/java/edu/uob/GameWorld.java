package edu.uob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameWorld {
    private Map<String, Location> locations;
    private Map<String, Player> players;
    private Location startLocation;
    private Location storeroom;

    public GameWorld() {
        this.locations = new HashMap<>();
        this.players = new HashMap<>();
    }

    public void addLocation(Location location) {
        this.locations.put(location.getName().toLowerCase(), location);

        // If this is the first location, set it as the start location
        if (this.startLocation == null && !location.getName().equalsIgnoreCase("storeroom")) {
            this.startLocation = location;
        }

        // If this is the storeroom, keep a reference to it
        if (location.getName().equalsIgnoreCase("storeroom")) {
            this.storeroom = location;
        }
    }

    public Location getLocation(String name) {
        return this.locations.get(name.toLowerCase());
    }

    public Set<Location> getAllLocations() {
        return new HashSet<>(this.locations.values());
    }

    public Location getStartLocation() {
        return this.startLocation;
    }

    public void setStartLocation(Location location) {
        this.startLocation = location;
    }

    public Location getStoreroom() {
        // Create storeroom if it doesn't exist
        if (this.storeroom == null) {
            this.storeroom = new Location("storeroom", "A storage room for game entities");
            this.addLocation(this.storeroom);
        }
        return this.storeroom;
    }

    public Player getPlayer(String username) {
        return this.players.get(username.toLowerCase());
    }

    public Player createPlayer(String username) {
        Player player = new Player(username);
        this.players.put(username.toLowerCase(), player);

        // Place the player at the start location
        player.setCurrentLocation(this.startLocation);

        return player;
    }

    public Set<Player> getAllPlayers() {
        return new HashSet<>(this.players.values());
    }

    // Move an entity to the storeroom (used when consuming entities)
    public void moveToStoreroom(GameEntity entity) {
        if (entity instanceof Artefact) {
            // If in a player's inventory, remove it first
            for (Player player : this.getAllPlayers()) {
                if (player.getFromInventory(entity.getName()) != null) {
                    player.removeFromInventory((Artefact) entity);
                }
            }

            // If in a location, remove it first
            for (Location location : this.getAllLocations()) {
                Artefact artefact = location.getArtefact(entity.getName());
                if (artefact != null) {
                    location.removeArtefact(artefact);
                }
            }

            // Add to storeroom
            this.getStoreroom().addArtefact((Artefact) entity);
        } else if (entity instanceof Character) {
            Character character = (Character) entity;
            if (character.getCurrentLocation() != null) {
                character.getCurrentLocation().removeCharacter(character);
            }
            character.setCurrentLocation(this.getStoreroom());
        }
        // Furniture and locations are handled differently
    }
}