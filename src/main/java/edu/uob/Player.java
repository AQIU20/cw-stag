package edu.uob;

import java.util.HashSet;
import java.util.Set;

public class Player extends Character {
    private Set<Artefact> inventory;
    private int health;
    private static final int MAX_HEALTH = 3;

    public Player(String name) {
        super(name, "A player in the game");
        this.inventory = new HashSet<>();
        this.health = MAX_HEALTH;
    }

    public boolean addToInventory(Artefact artefact) {
        return this.inventory.add(artefact);
    }

    public boolean removeFromInventory(Artefact artefact) {
        return this.inventory.remove(artefact);
    }

    public Set<Artefact> getInventory() {
        return new HashSet<>(this.inventory);
    }

    public Artefact getFromInventory(String artefactName) {
        for (Artefact artefact : this.inventory) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return artefact;
            }
        }
        return null;
    }

    public String getInventoryDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Inventory for ").append(this.getName()).append(":\n");

        if (this.inventory.isEmpty()) {
            description.append("Your inventory is empty");
        } else {
            for (Artefact artefact : this.inventory) {
                description.append("- ").append(artefact.getName());
                description.append(" (").append(artefact.getDescription()).append(")\n");
            }
        }

        return description.toString();
    }

    public int getHealth() {
        return this.health;
    }

    public void decreaseHealth() {
        if (this.health > 0) {
            this.health--;
        }

        if (this.health == 0) {
            this.handleDeath();
        }
    }

    public void increaseHealth() {
        if (this.health < MAX_HEALTH) {
            this.health++;
        }
    }

    public boolean isDead() {
        return this.health == 0;
    }

    private void handleDeath() {
        Location currentLocation = this.getCurrentLocation();

        // Drop all items from inventory to current location
        for (Artefact artefact : this.getInventory()) {
            this.removeFromInventory(artefact);
            currentLocation.addArtefact(artefact);
        }

        // Health will be restored when player is moved to start location
        // by the CommandHandler after death
    }

    public void resetHealth() {
        this.health = MAX_HEALTH;
    }
}