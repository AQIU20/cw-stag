package edu.uob;

import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
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

        Iterator<Graph> sectionsIt = wholeDocument.getSubgraphs().iterator();
        if (sectionsIt.hasNext()) {
            Graph firstSection = sectionsIt.next();
            this.parseLocations(firstSection);
        }
        if (sectionsIt.hasNext()) {
            Graph secondSection = sectionsIt.next();
            this.parsePaths(secondSection);
        }
    }

    private void parseLocations(Graph locationsGraph) {
        // 直接使用迭代器遍历子图
        Iterator<Graph> locSubgraphsIt = locationsGraph.getSubgraphs().iterator();
        while (locSubgraphsIt.hasNext()) {
            Graph locationGraph = locSubgraphsIt.next();
            // 获取 location 详细信息（假定每个子图至少有一个节点）
            Node locationNode = locationGraph.getNodes(false).iterator().next();
            String locationName = locationNode.getId().getId();
            String locationDescription = locationNode.getAttribute("description");

            // 创建并存储 location
            Location location = new Location(locationName, locationDescription);
            this.gameWorld.addLocation(location);
            this.locationMap.put(locationName.toLowerCase(), location);

            // 处理该 location 内的实体
            this.parseEntitiesInLocation(locationGraph, location);
        }
    }

    private void parseEntitiesInLocation(Graph locationGraph, Location location) {
        // 直接使用迭代器遍历子图
        Iterator<Graph> entityGraphsIt = locationGraph.getSubgraphs().iterator();
        while (entityGraphsIt.hasNext()) {
            Graph entityGraph = entityGraphsIt.next();
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
                        System.err.printf("Unknown entity type: %s%n", entityType);
                }
            }
        }
    }

    private void parsePaths(Graph pathsGraph) {
        // edges
        Iterator<Edge> edgesIt = pathsGraph.getEdges().iterator();
        while (edgesIt.hasNext()) {
            Edge edge = edgesIt.next();
            String fromLocationName = edge.getSource().getNode().getId().getId();
            String toLocationName = edge.getTarget().getNode().getId().getId();

            Location fromLocation = this.locationMap.get(fromLocationName.toLowerCase());
            Location toLocation = this.locationMap.get(toLocationName.toLowerCase());

            if (fromLocation != null && toLocation != null) {
                GamePath gamePath = new GamePath(fromLocation, toLocation);
                fromLocation.addPath(gamePath);
            } else {
                System.err.printf("Cannot create path from %s to %s%n", fromLocationName, toLocationName);
            }
        }
    }
}