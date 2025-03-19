package edu.uob;

import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EntityParser {
    private GameWorld gameWorld;
    private Map<String, Location> locationMap;

    public EntityParser(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.locationMap = new HashMap<>();
    }

    public void parseEntities(File entitiesFile) throws FileNotFoundException, ParseException {
        Parser parser = new Parser();
        FileReader reader = new FileReader(entitiesFile);
        parser.parse(reader);
        Graph wholeDocument = parser.getGraphs().get(0);
        ArrayList<Graph> sections = wholeDocument.getSubgraphs();

        // Parse locations (first subgraph)
        parseLocations(sections.get(0));

        // Parse paths (second subgraph)
        parsePaths(sections.get(1));
    }

    private void parseLocations(Graph locationsGraph) {
        ArrayList<Graph> locationSubgraphs = locationsGraph.getSubgraphs();

        for (Graph locationGraph : locationSubgraphs) {
            // Get location details
            Node locationNode = locationGraph.getNodes(false).get(0);
            String locationName = locationNode.getId().getId();
            String locationDescription = locationNode.getAttribute("description");

            // Create location
            Location location = new Location(locationName, locationDescription);
            this.gameWorld.addLocation(location);
            this.locationMap.put(locationName.toLowerCase(), location);

            // Process entities within the location
            parseEntitiesInLocation(locationGraph, location);
        }
    }

    private void parseEntitiesInLocation(Graph locationGraph, Location location) {
        ArrayList<Graph> entityGraphs = locationGraph.getSubgraphs();

        for (Graph entityGraph : entityGraphs) {
            String entityType = entityGraph.getId().getId().toLowerCase();

            for (Node entityNode : entityGraph.getNodes(false)) {
                String entityName = entityNode.getId().getId();
                String entityDescription = entityNode.getAttribute("description");

                switch (entityType) {
                    case "characters":
                        Character character = new Character(entityName, entityDescription);
                        character.setCurrentLocation(location);
                        break;
                    case "artefacts":
                        Artefact artefact = new Artefact(entityName, entityDescription);
                        location.addArtefact(artefact);
                        break;
                    case "furniture":
                        Furniture furniture = new Furniture(entityName, entityDescription);
                        location.addFurniture(furniture);
                        break;
                    default:
                        System.err.println("Unknown entity type: " + entityType);
                }
            }
        }
    }

    private void parsePaths(Graph pathsGraph) {
        ArrayList<Edge> edges = pathsGraph.getEdges();

        for (Edge edge : edges) {
            String fromLocationName = edge.getSource().getNode().getId().getId();
            String toLocationName = edge.getTarget().getNode().getId().getId();

            Location fromLocation = this.locationMap.get(fromLocationName.toLowerCase());
            Location toLocation = this.locationMap.get(toLocationName.toLowerCase());

            if (fromLocation != null && toLocation != null) {
                GamePath gamePath = new GamePath(fromLocation, toLocation);
                fromLocation.addPath(gamePath);
            } else {
                System.err.println("Cannot create path from " + fromLocationName + " to " + toLocationName);
            }
        }
    }
}