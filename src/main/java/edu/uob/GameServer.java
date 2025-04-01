package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class GameServer {
    private static final char END_OF_TRANSMISSION = 4;
    private GameWorld gameWorld;
    private GameActionManager actionManager;
    private static final Set<String> BUILT_IN_COMMANDS = new HashSet<>(
            java.util.Arrays.asList("inventory", "inv", "get", "drop", "goto", "look", "health"));

    public static void main(String[] args) throws IOException {
        StringBuilder entityPathBuilder = new StringBuilder();
        entityPathBuilder.append("config");
        entityPathBuilder.append(File.separator);
        entityPathBuilder.append("basic-entities.dot");
        File entitiesFile = new File(entityPathBuilder.toString());

        StringBuilder actionPathBuilder = new StringBuilder();
        actionPathBuilder.append("config");
        actionPathBuilder.append(File.separator);
        actionPathBuilder.append("basic-actions.xml");
        File actionsFile = new File(actionPathBuilder.toString());

        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
     * Instantiates a new server instance, specifying a game with some configuration files.
     */
    public GameServer(File entitiesFile, File actionsFile) {
        try {
            // Initialize the game world with entities
            this.gameWorld = new GameWorld();
            EntityParser entityParser = new EntityParser(this.gameWorld);
            entityParser.parseEntities(entitiesFile);

            // Initialize the action system
            ActionParser actionParser = new ActionParser();
            this.actionManager = actionParser.parseActions(actionsFile);
        } catch (Exception e) {
            System.err.printf("Error initializing game server: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method handles all incoming game commands and carries out the corresponding actions.
     *
     * @param command The incoming command to be processed
     */
    public String handleCommand(String command) {
        try {
            // Extract username and actual command
            int colonIndex = command.indexOf(':');
            if (colonIndex == -1) {
                return "Invalid command format. Please use 'username: command'";
            }

            String username = command.substring(0, colonIndex).trim();
            int startIndex = colonIndex;
            startIndex++; // 使用单独的运算避免使用 “+”
            String userCommand = command.substring(startIndex).trim();

            // Validate username (only letters, spaces, apostrophes, hyphens)
            if (!this.isValidUsername(username)) {
                return "Invalid username. Username can only contain letters, spaces, apostrophes, and hyphens.";
            }

            // Get or create player
            Player player = this.gameWorld.getPlayer(username);
            if (player == null) {
                player = this.gameWorld.createPlayer(username);
            }

            // Handle player death if needed
            if (player.isDead()) {
                player.resetHealth();
                player.setCurrentLocation(this.gameWorld.getStartLocation());
                return "You have been returned to the start of the game after your death.";
            }

            // Parse and execute the command
            return this.executeCommand(player, userCommand);

        } catch (Exception e) {
            e.printStackTrace();
            return String.format("Error processing command: %s", e.getMessage());
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z\\s'-]+$");
    }

    /**
     * split to words
     */
    private List<String> tokenizeCommand(String command) {
        List<String> tokens = new LinkedList<>();
        Scanner scanner = new Scanner(command);
        while (scanner.hasNext()) {
            tokens.add(scanner.next());
        }
        scanner.close();
        return tokens;
    }

    /**
     * find all tokens
     */
    private Set<Object> detectTriggers(String command) {
        Set<Object> detected = new HashSet<>();

        for (String builtin : BUILT_IN_COMMANDS) {
            if (commandMatchesTrigger(command, builtin)) {
                detected.add(builtin.toLowerCase());
            }
        }

        for (GameAction action : this.actionManager.getAllActions()) {
            for (String trigger : action.getTriggers()) {
                if (commandMatchesTrigger(command, trigger)) {
                    detected.add(action);
                    break;
                }
            }
        }
        return detected;
    }

    /**
     * find Trigger
     */
    private boolean commandMatchesTrigger(String command, String trigger) {
        String pattern = String.format(".*\\b%s\\b.*", trigger.toLowerCase());
        return command.matches(pattern);
    }

    /**
     * stop different trigger
     */
    private String executeCommand(Player player, String command) {
        // Convert to lowercase for case-insensitive matching
        String lowerCommand = command.toLowerCase();
        List<String> words = this.tokenizeCommand(lowerCommand);
        if (words.isEmpty()) {
            return "Please enter a command.";
        }

        Set<Object> detected = this.detectTriggers(lowerCommand);
        if (detected.size() > 1) {
            return "Ambiguous command. Your command contains triggers for different actions.";
        }

        if (BUILT_IN_COMMANDS.contains(words.get(0))) {
            return this.executeBuiltInCommand(player, words.get(0), words.subList(1, words.size()));
        }
        return this.executeCustomAction(player, words);
    }

    private String executeBuiltInCommand(Player player, String command, List<String> args) {
        switch (command) {
            case "inventory":
            case "inv":
                return player.getInventoryDescription();

            case "get":
                return this.handleGetCommand(player, args);

            case "drop":
                return this.handleDropCommand(player, args);

            case "goto":
                return this.handleGotoCommand(player, args);

            case "look":
                return player.getCurrentLocation().generateDescription();

            case "health":
                return String.format("Your current health is: %d", player.getHealth());

            default:
                return String.format("Unknown built-in command: %s", command);
        }
    }

    private String handleGetCommand(Player player, List<String> args) {
        if (args.isEmpty()) {
            return "What do you want to get?";
        }

        // artefact
        Location currentLocation = player.getCurrentLocation();
        Artefact foundArtefact = null;
        String matchedName = null;

        for (String arg : args) {
            Artefact artefact = currentLocation.getArtefact(arg);
            if (artefact != null) {
                if (foundArtefact != null) {
                    return "You can only get one item at a time.";
                }
                foundArtefact = artefact;
                matchedName = arg;
            }
        }

        if (foundArtefact == null) {
            if (args.size() == 1) {
                StringBuilder errorBuilder = new StringBuilder();
                errorBuilder.append("There is no ");
                errorBuilder.append(args.get(0));
                errorBuilder.append(" here.");
                return errorBuilder.toString();
            } else {
                return "I don't see that item here.";
            }
        }

        currentLocation.removeArtefact(foundArtefact);
        player.addToInventory(foundArtefact);

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("You picked up the ");
        responseBuilder.append(matchedName);
        responseBuilder.append(".");
        return responseBuilder.toString();
    }

    private String handleDropCommand(Player player, List<String> args) {
        if (args.isEmpty()) {
            return "What do you want to drop?";
        }

        Artefact foundArtefact = null;
        String matchedName = null;

        for (String arg : args) {
            Artefact artefact = player.getFromInventory(arg);
            if (artefact != null) {
                if (foundArtefact != null) {
                    return "You can only drop one item at a time.";
                }
                foundArtefact = artefact;
                matchedName = arg;
            }
        }

        if (foundArtefact == null) {
            if (args.size() == 1) {
                StringBuilder errorBuilder = new StringBuilder();
                errorBuilder.append("You don't have a ");
                errorBuilder.append(args.get(0));
                errorBuilder.append(" in your inventory.");
                return errorBuilder.toString();
            } else {
                return "I don't see that item in your inventory.";
            }
        }

        player.removeFromInventory(foundArtefact);
        player.getCurrentLocation().addArtefact(foundArtefact);

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("You dropped the ");
        responseBuilder.append(matchedName);
        responseBuilder.append(".");
        return responseBuilder.toString();
    }

    private String handleGotoCommand(Player player, List<String> args) {
        if (args.isEmpty()) {
            return "Where do you want to go?";
        }

        Location currentLocation = player.getCurrentLocation();
        Location foundDestination = null;
        String matchedName = null;
        GamePath foundPath = null;

        for (String arg : args) {
            Location destination = this.gameWorld.getLocation(arg);
            if (destination != null) {
                GamePath path = currentLocation.getPathTo(destination);
                if (path != null) {
                    if (foundDestination != null) {
                        return "You can only go to one location at a time.";
                    }
                    foundDestination = destination;
                    matchedName = arg;
                    foundPath = path;
                }
            }
        }

        if (foundDestination == null) {
            if (args.size() == 1) {
                Location destination = this.gameWorld.getLocation(args.get(0));
                if (destination != null) {
                    StringBuilder errorBuilder = new StringBuilder();
                    errorBuilder.append("There is no path from here to ");
                    errorBuilder.append(args.get(0));
                    errorBuilder.append(".");
                    return errorBuilder.toString();
                } else {
                    StringBuilder errorBuilder = new StringBuilder();
                    errorBuilder.append("There is no location called ");
                    errorBuilder.append(args.get(0));
                    errorBuilder.append(".");
                    return errorBuilder.toString();
                }
            } else {
                return "I don't understand where you want to go.";
            }
        }

        player.setCurrentLocation(foundDestination);

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("You have moved to ");
        responseBuilder.append(matchedName);
        responseBuilder.append(".\n");
        responseBuilder.append(foundDestination.generateDescription());
        return responseBuilder.toString();
    }

    private String executeCustomAction(Player player, List<String> words) {
        if (words.isEmpty()) {
            return "Please enter a command.";
        }

        // Complete command
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                commandBuilder.append(" ");
            }
            commandBuilder.append(words.get(i).toLowerCase());
        }
        String fullCommand = commandBuilder.toString();
        System.out.printf("Full command: %s%n", fullCommand);

        // check trigger
        Set<Object> detected = this.detectTriggers(fullCommand);
        if (detected.size() > 1) {
            return "Ambiguous command. Your command contains triggers for different actions.";
        }

        // extract words
        Set<String> potentialSubjects = new HashSet<>();
        for (String word : words) {
            potentialSubjects.add(word.toLowerCase());
        }
        System.out.printf("Potential subjects: %s%n", potentialSubjects);

        List<GameAction> candidateActions = this.actionManager.findActionsByTrigger(fullCommand);
        System.out.printf("Found %d actions matching triggers in the command%n", candidateActions.size());

        if (candidateActions.isEmpty()) {
            return "I don't understand what you want to do.";
        }

        Set<GameAction> uniqueActions = new HashSet<>(candidateActions);
        if (uniqueActions.size() > 1) {
            return "Ambiguous command. Your command contains triggers for different actions.";
        }

        GameAction bestAction = uniqueActions.iterator().next();
        System.out.printf("Selected action with triggers: %s%n", bestAction.getTriggers());

        // check at least one subject
        boolean atLeastOneSubjectMentioned = false;
        Iterator<String> requiredSubjectIterator = bestAction.getSubjects().iterator();
        while (requiredSubjectIterator.hasNext() && !atLeastOneSubjectMentioned) {
            String requiredSubject = requiredSubjectIterator.next();
            if (potentialSubjects.contains(requiredSubject.toLowerCase())) {
                atLeastOneSubjectMentioned = true;
            }
        }
        if (!atLeastOneSubjectMentioned && !bestAction.getSubjects().isEmpty()) {
            return "Your command must include at least one subject of the action.";
        }

        // check necessary subject
        Iterator<String> subjectNameIterator = bestAction.getSubjects().iterator();
        while (subjectNameIterator.hasNext()) {
            String subjectName = subjectNameIterator.next();
            if (!this.isEntityAvailableToPlayer(player, subjectName)) {
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("You don't have access to ");
                messageBuilder.append(subjectName);
                messageBuilder.append(".");
                return messageBuilder.toString();
            }
        }

        return this.performAction(player, bestAction);
    }

    private void sortStringList(List<String> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    String temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * check entity available
     */
    private boolean isEntityAvailableToPlayer(Player player, String entityName) {
        Location currentLocation = player.getCurrentLocation();

        if (player.getFromInventory(entityName) != null) {
            System.out.printf("Entity '%s' found in player inventory%n", entityName);
            return true;
        }

        if (currentLocation.getArtefact(entityName) != null) {
            System.out.printf("Entity '%s' found as artefact in current location%n", entityName);
            return true;
        }

        Iterator<Furniture> furnitureIterator = currentLocation.getFurniture().iterator();
        while (furnitureIterator.hasNext()) {
            Furniture furniture = furnitureIterator.next();
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                System.out.printf("Entity '%s' found as furniture in current location%n", entityName);
                return true;
            }
        }

        Iterator<Character> characterIterator = currentLocation.getCharacters().iterator();
        while (characterIterator.hasNext()) {
            Character character = characterIterator.next();
            if (character.getName().equalsIgnoreCase(entityName)) {
                System.out.printf("Entity '%s' found as character in current location%n", entityName);
                return true;
            }
        }

        if (currentLocation.getName().equalsIgnoreCase(entityName)) {
            System.out.printf("Entity '%s' is the current location%n", entityName);
            return true;
        }

        System.out.printf("Entity '%s' is NOT available to player%n", entityName);
        return false;
    }

    private String performAction(Player player, GameAction action) {
        Location currentLocation = player.getCurrentLocation();

        for (String entityName : action.getConsumed()) {
            if (entityName.equalsIgnoreCase("health")) {
                player.decreaseHealth();
                if (player.isDead()) {
                    player.resetHealth();
                    player.setCurrentLocation(this.gameWorld.getStartLocation());
                    return String.format("%s%nYou died and lost all of your items, you must return to the start of the game", action.getNarration());
                }
                continue;
            }

            Location locationToConsume = this.gameWorld.getLocation(entityName);
            if (locationToConsume != null) {
                GamePath pathToRemove = currentLocation.getPathTo(locationToConsume);
                if (pathToRemove != null) {
                    currentLocation.removePath(pathToRemove);
                }
                continue;
            }

            GameEntity entityToConsume = null;
            Artefact artefactInInventory = player.getFromInventory(entityName);
            if (artefactInInventory != null) {
                entityToConsume = artefactInInventory;
                player.removeFromInventory(artefactInInventory);
            } else {
                Artefact artefactInLocation = currentLocation.getArtefact(entityName);
                if (artefactInLocation != null) {
                    entityToConsume = artefactInLocation;
                    currentLocation.removeArtefact(artefactInLocation);
                }
            }

            if (entityToConsume != null) {
                this.gameWorld.moveToStoreroom(entityToConsume);
            }
        }

        for (String entityName : action.getProduced()) {
            if (entityName.equalsIgnoreCase("health")) {
                player.increaseHealth();
                continue;
            }

            Location locationToProduce = this.gameWorld.getLocation(entityName);
            if (locationToProduce != null) {
                GamePath newPath = new GamePath(currentLocation, locationToProduce);
                currentLocation.addPath(newPath);
                continue;
            }

            Artefact artefactInStoreroom = this.gameWorld.getStoreroom().getArtefact(entityName);
            if (artefactInStoreroom != null) {
                this.gameWorld.getStoreroom().removeArtefact(artefactInStoreroom);
                currentLocation.addArtefact(artefactInStoreroom);
            }
        }

        return action.getNarration();
    }

    /**
     * Starts a *blocking* socket server listening for new connections.
     *
     * @param portNumber The port to listen on.
     * @throws IOException If any IO related operation fails.
     */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.printf("Server listening on port %d%n", portNumber);
            while (!Thread.interrupted()) {
                try {
                    this.blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
     * Handles an incoming connection from the socket server.
     *
     * @param serverSocket The client socket to read/write from.
     * @throws IOException If any IO related operation fails.
     */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if (incomingCommand != null) {
                System.out.printf("Received message from %s%n", incomingCommand);
                String result = this.handleCommand(incomingCommand);
                writer.write(result);
                writer.write(String.format("%n%c%n", END_OF_TRANSMISSION));
                writer.flush();
            }
        }
    }
}
