package edu.uob;

public class Furniture extends GameEntity {

    public Furniture(String name, String description) {
        super(name, description);
    }

    // Furniture are fixed entities that cannot be collected
    // Additional functionality can be added here if needed
}