package edu.uob;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Location extends GameEntity {
    private Set<Character> characters;
    private Set<Artefact> artefacts;
    private Set<Furniture> furniture;
    private Set<GamePath> paths;

    public Location(String name, String description) {
        super(name, description);
        this.characters = new HashSet<>();
        this.artefacts = new HashSet<>();
        this.furniture = new HashSet<>();
        this.paths = new HashSet<>();
    }

    public void addCharacter(Character character) {
        this.characters.add(character);
    }

    public void removeCharacter(Character character) {
        this.characters.remove(character);
    }

    public void addArtefact(Artefact artefact) {
        this.artefacts.add(artefact);
    }

    public void removeArtefact(Artefact artefact) {
        this.artefacts.remove(artefact);
    }

    public void addFurniture(Furniture furniture) {
        this.furniture.add(furniture);
    }

    public Artefact getArtefact(String artefactName) {
        for (Artefact artefact : this.artefacts) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return artefact;
            }
        }
        return null;
    }

    public void addPath(GamePath path) {  // 原来是 public void addPath(Path path)
        this.paths.add(path);
    }

    public void removePath(GamePath path) {  // 原来是 public void removePath(Path path)
        this.paths.remove(path);
    }

    public GamePath getPathTo(Location destination) {
        for (GamePath path : this.paths) {
            if (path.getDestination().equals(destination)) {
                return path;
            }
        }
        return null;
    }

    public Set<Character> getCharacters() {
        return new HashSet<>(this.characters);
    }

    public Set<Artefact> getArtefacts() {
        return new HashSet<>(this.artefacts);
    }

    public Set<Furniture> getFurniture() {
        return new HashSet<>(this.furniture);
    }

    public Set<GamePath> getPaths() {
        return new HashSet<>(this.paths);
    }

    public String generateDescription() {
        StringBuilder description = new StringBuilder();
        description.append("You are in the ").append(this.getName()).append(". ");
        description.append(this.getDescription()).append("\n");

        // List the furniture
        if (!this.furniture.isEmpty()) {
            description.append("\nFurniture: ");
            boolean first = true;
            for (Furniture item : this.furniture) {
                if (!first) {
                    description.append(", ");
                }
                description.append(item.getName()).append(" (").append(item.getDescription()).append(")");
                first = false;
            }
        }

        // List the artefacts
        if (!this.artefacts.isEmpty()) {
            description.append("\nArtefacts: ");
            boolean first = true;
            for (Artefact item : this.artefacts) {
                if (!first) {
                    description.append(", ");
                }
                description.append(item.getName()).append(" (").append(item.getDescription()).append(")");
                first = false;
            }
        }

        // List the characters (excluding players which will be handled separately)
        Set<Character> nonPlayerCharacters = new HashSet<>();
        for (Character character : this.characters) {
            if (!(character instanceof Player)) {
                nonPlayerCharacters.add(character);
            }
        }

        if (!nonPlayerCharacters.isEmpty()) {
            description.append("\nCharacters: ");
            boolean first = true;
            for (Character character : nonPlayerCharacters) {
                if (!first) {
                    description.append(", ");
                }
                description.append(character.getName()).append(" (").append(character.getDescription()).append(")");
                first = false;
            }
        }

        // List other players
        Set<Player> players = new HashSet<>();
        for (Character character : this.characters) {
            if (character instanceof Player) {
                players.add((Player) character);
            }
        }

        if (!players.isEmpty()) {
            description.append("\nPlayers: ");
            boolean first = true;
            for (Player player : players) {
                if (!first) {
                    description.append(", ");
                }
                description.append(player.getName());
                first = false;
            }
        }

        // List the paths to other locations
        if (!this.paths.isEmpty()) {
            description.append("\nPaths: ");
            boolean first = true;
            for (GamePath path : this.paths) {
                if (!first) {
                    description.append(", ");
                }
                description.append(path.getDestination().getName());
                first = false;
            }
        }

        return description.toString();
    }
}