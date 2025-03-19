package edu.uob;

public class GamePath {
    private Location source;
    private Location destination;

    public GamePath(Location source, Location destination) {
        this.source = source;
        this.destination = destination;
    }

    public Location getSource() {
        return this.source;
    }

    public Location getDestination() {
        return this.destination;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GamePath other = (GamePath) obj;
        return this.source.equals(other.source) && this.destination.equals(other.destination);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (this.source != null ? this.source.hashCode() : 0);
        result = 31 * result + (this.destination != null ? this.destination.hashCode() : 0);
        return result;
    }
}