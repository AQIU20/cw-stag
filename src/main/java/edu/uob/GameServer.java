package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GameServer {
    private GameWorld gameWorld;
    private GameActionManager actionManager;
    private static final Set<String> BUILT_IN_COMMANDS = new HashSet<>(
            Arrays.asList("inventory", "inv", "get", "drop", "goto", "look", "health"));

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Instanciates a new server instance, specifying a game with some configuration files
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
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
            System.err.println("Error initializing game server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
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
            String userCommand = command.substring(colonIndex + 1).trim();

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
                return "You died and lost all of your items, you must return to the start of the game";
            }

            // Parse and execute the command
            return this.executeCommand(player, userCommand);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing command: " + e.getMessage();
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z\\s'-]+$");
    }

    private String executeCommand(Player player, String command) {
        // Convert to lowercase for case-insensitive matching
        String lowerCommand = command.toLowerCase();

        // Split the command into words
        String[] words = lowerCommand.split("\\s+");
        if (words.length == 0) {
            return "Please enter a command.";
        }

        // Check if it's a built-in command
        String firstWord = words[0];
        if (BUILT_IN_COMMANDS.contains(firstWord)) {
            return this.executeBuiltInCommand(player, firstWord, Arrays.copyOfRange(words, 1, words.length));
        }

        // If not a built-in command, try to match with a custom action
        return this.executeCustomAction(player, words);
    }

    private String executeBuiltInCommand(Player player, String command, String[] args) {
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
                return "Your current health is: " + player.getHealth();

            default:
                return "Unknown built-in command: " + command;
        }
    }

    private String handleGetCommand(Player player, String[] args) {
        if (args.length == 0) {
            return "What do you want to get?";
        }

        // Extraneous entities check
        if (args.length > 1) {
            return "You can only get one item at a time.";
        }

        String artefactName = args[0];
        Location currentLocation = player.getCurrentLocation();
        Artefact artefact = currentLocation.getArtefact(artefactName);

        if (artefact == null) {
            return "There is no " + artefactName + " here.";
        }

        currentLocation.removeArtefact(artefact);
        player.addToInventory(artefact);

        return "You picked up the " + artefactName + ".";
    }

    private String handleDropCommand(Player player, String[] args) {
        if (args.length == 0) {
            return "What do you want to drop?";
        }

        // Extraneous entities check
        if (args.length > 1) {
            return "You can only drop one item at a time.";
        }

        String artefactName = args[0];
        Artefact artefact = player.getFromInventory(artefactName);

        if (artefact == null) {
            return "You don't have a " + artefactName + " in your inventory.";
        }

        player.removeFromInventory(artefact);
        player.getCurrentLocation().addArtefact(artefact);

        return "You dropped the " + artefactName + ".";
    }

    private String handleGotoCommand(Player player, String[] args) {
        if (args.length == 0) {
            return "Where do you want to go?";
        }

        // Extraneous entities check
        if (args.length > 1) {
            return "You can only go to one location at a time.";
        }

        String locationName = args[0];
        Location currentLocation = player.getCurrentLocation();
        Location destination = this.gameWorld.getLocation(locationName);

        if (destination == null) {
            return "There is no location called " + locationName + ".";
        }

        // Check if there's a path to the destination
        GamePath gamePath = (GamePath) currentLocation.getPathTo(destination);
        if (gamePath == null) {
            return "There is no path from here to " + locationName + ".";
        }

        // Move player to new location
        player.setCurrentLocation(destination);

        return "You have moved to " + locationName + ".\n" + destination.generateDescription();
    }

    private String executeCustomAction(Player player, String[] words) {
        if (words.length == 0) {
            return "Please enter a command.";
        }

        // Extract potential triggers and subjects
        Set<String> potentialTriggers = new HashSet<>();
        Set<String> potentialSubjects = new HashSet<>();

        // Simple approach: consider each word as a potential trigger or subject
        for (String word : words) {
            if (!this.isCommonWord(word)) {
                potentialTriggers.add(word);
                potentialSubjects.add(word);
            }
        }

        // Try each potential trigger
        List<GameAction> matchingActions = null;
        String usedTrigger = null;

        for (String trigger : potentialTriggers) {
            List<GameAction> actions = this.actionManager.findMatchingActions(trigger, potentialSubjects);
            if (!actions.isEmpty()) {
                if (matchingActions != null) {
                    // Multiple matching actions found
                    return "Ambiguous command. Please be more specific.";
                }
                matchingActions = actions;
                usedTrigger = trigger;
            }
        }

        if (matchingActions == null || matchingActions.isEmpty()) {
            return "I don't understand what you want to do.";
        }

        if (matchingActions.size() > 1) {
            return "There is more than one '" + usedTrigger + "' action possible - which one do you want to perform?";
        }

        // Execute the matching action
        GameAction action = matchingActions.get(0);
        return this.performAction(player, action);
    }

    private boolean isCommonWord(String word) {
        Set<String> commonWords = new HashSet<>(Arrays.asList(
                "the", "a", "an", "and", "with", "using", "to", "at", "in", "on", "by", "for", "from",
                "of", "or", "it", "its", "this", "that", "these", "those", "my", "your", "his", "her", "their"));

        boolean isCommon = commonWords.contains(word.toLowerCase());
        System.out.println("Word '" + word + "' is common word? " + isCommon);
        return isCommon;
    }

    private String performAction(Player player, GameAction action) {
        Location currentLocation = player.getCurrentLocation();

        // Check if all subject entities are available to the player
        for (String subjectName : action.getSubjects()) {
            boolean available = false;

            // Check player inventory
            if (player.getFromInventory(subjectName) != null) {
                available = true;
            }

            // Check current location
            if (!available) {
                // Check artefacts
                if (currentLocation.getArtefact(subjectName) != null) {
                    available = true;
                }

                // Check furniture
                if (!available) {
                    for (Furniture furniture : currentLocation.getFurniture()) {
                        if (furniture.getName().equalsIgnoreCase(subjectName)) {
                            available = true;
                            break;
                        }
                    }
                }

                // Check characters in location
                if (!available) {
                    for (Character character : currentLocation.getCharacters()) {
                        if (character.getName().equalsIgnoreCase(subjectName)) {
                            available = true;
                            break;
                        }
                    }
                }

                // Check if the location itself is a subject
                if (!available && currentLocation.getName().equalsIgnoreCase(subjectName)) {
                    available = true;
                }
            }

            if (!available) {
                return "You don't have access to " + subjectName + ".";
            }
        }

        // Handle consumed entities
        for (String entityName : action.getConsumed()) {
            if (entityName.equalsIgnoreCase("health")) {
                player.decreaseHealth();
                if (player.isDead()) {
                    return action.getNarration() + "\nYou died and lost all of your items, you must return to the start of the game";
                }
                continue;
            }

            // Check if it's a location (path removal)
            Location locationToConsume = this.gameWorld.getLocation(entityName);
            if (locationToConsume != null) {
                GamePath gamePathToRemove = (GamePath) currentLocation.getPathTo(locationToConsume);
                if (gamePathToRemove != null) {
                    currentLocation.removePath(gamePathToRemove);
                }
                continue;
            }

            // Find entity in player inventory or current location
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

                // Check for characters or furniture
                // Implementation depends on how you want to handle consuming these types
            }

            if (entityToConsume != null) {
                this.gameWorld.moveToStoreroom(entityToConsume);
            }
        }

        // Handle produced entities
        for (String entityName : action.getProduced()) {
            if (entityName.equalsIgnoreCase("health")) {
                player.increaseHealth();
                continue;
            }

            // Check if it's a location (path creation)
            Location locationToProduce = this.gameWorld.getLocation(entityName);
            if (locationToProduce != null) {
                GamePath newGamePath = new GamePath(currentLocation, locationToProduce);
                currentLocation.addPath(newGamePath);
                continue;
            }

            // Find entity in storeroom
            Artefact artefactInStoreroom = this.gameWorld.getStoreroom().getArtefact(entityName);
            if (artefactInStoreroom != null) {
                this.gameWorld.getStoreroom().removeArtefact(artefactInStoreroom);
                currentLocation.addArtefact(artefactInStoreroom);
            }

            // Note: Handle other entity types here if needed
        }

        return action.getNarration();
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
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
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
