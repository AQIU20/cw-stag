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
import java.nio.file.Paths;
import java.util.*;

public final class GameServer {
    private GameWorld gameWorld;
    private GameActionManager actionManager;
    private static final Set<String> BUILT_IN_COMMANDS = new HashSet<>(
            Arrays.asList("inventory", "inv", "get", "drop", "goto", "look", "health"));

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
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

            // Handle player death if needed - this is now redundant since we handle it in performAction
            // but we keep it as a safety check
            if (player.isDead()) {
                player.resetHealth();
                player.setCurrentLocation(this.gameWorld.getStartLocation());
                return "You have been returned to the start of the game after your death.";
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

        // Filter out common words
        List<String> filteredArgs = new LinkedList<>();
        for (int i = 0; i < args.length; i++) {
            if (!this.isCommonWord(args[i])) {
                filteredArgs.add(args[i]);
            }
        }

        // Extraneous entities check (after filtering common words)
        if (filteredArgs.size() > 1) {
            return "You can only get one item at a time.";
        }

        // If no non-common words, return error
        if (filteredArgs.isEmpty()) {
            return "What do you want to get?";
        }

        String artefactName = filteredArgs.get(0);
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

        // Filter out common words
        List<String> filteredArgs = new LinkedList<>();
        for (int i = 0; i < args.length; i++) {
            if (!this.isCommonWord(args[i])) {
                filteredArgs.add(args[i]);
            }
        }

        // Extraneous entities check (after filtering common words)
        if (filteredArgs.size() > 1) {
            return "You can only drop one item at a time.";
        }

        // If no non-common words, return error
        if (filteredArgs.isEmpty()) {
            return "What do you want to drop?";
        }

        String artefactName = filteredArgs.get(0);
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

        // Filter out common words
        List<String> filteredArgs = new LinkedList<>();
        for (int i = 0; i < args.length; i++) {
            if (!this.isCommonWord(args[i])) {
                filteredArgs.add(args[i]);
            }
        }

        // Extraneous entities check (after filtering common words)
        if (filteredArgs.size() > 1) {
            return "You can only go to one location at a time.";
        }

        // If no non-common words, return error
        if (filteredArgs.isEmpty()) {
            return "Where do you want to go?";
        }

        String locationName = filteredArgs.get(0);
        Location currentLocation = player.getCurrentLocation();
        Location destination = this.gameWorld.getLocation(locationName);

        if (destination == null) {
            return "There is no location called " + locationName + ".";
        }

        // Check if there's a path to the destination
        GamePath path = currentLocation.getPathTo(destination);
        if (path == null) {
            return "There is no path from here to " + locationName + ".";
        }

        // Move player to new location
        player.setCurrentLocation(destination);

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("You have moved to ");
        responseBuilder.append(locationName);
        responseBuilder.append(".\n");
        responseBuilder.append(destination.generateDescription());
        return responseBuilder.toString();
    }

    private String executeCustomAction(Player player, String[] words) {
        if (words.length == 0) {
            return "Please enter a command.";
        }

        // Combine all words to form the complete command
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                commandBuilder.append(" ");
            }
            commandBuilder.append(words[i].toLowerCase());
        }
        String fullCommand = commandBuilder.toString();
        System.out.println("Full command: " + fullCommand);

        // Extract all non-trigger words as potential subjects
        Set<String> potentialSubjects = new HashSet<>();
        for (String word : words) {
            potentialSubjects.add(word.toLowerCase());
        }

        System.out.println("Potential subjects: " + potentialSubjects);

        // Find actions that match the command string
        List<GameAction> candidateActions = this.actionManager.findActionsByTrigger(fullCommand);
        System.out.println("Found " + candidateActions.size() + " actions matching triggers in the command");

        if (candidateActions.isEmpty()) {
            return "I don't understand what you want to do.";
        }

        // Group candidate actions by their associated action (based on subjects and consumed/produced)
        Map<String, List<GameAction>> actionGroups = new HashMap<>();

        Iterator<GameAction> actionIterator = candidateActions.iterator();
        while (actionIterator.hasNext()) {
            GameAction action = actionIterator.next();

            // Create a unique key for this action based on its subjects, consumed, and produced entities
            StringBuilder keyBuilder = new StringBuilder();

            // Add subjects to key
            List<String> sortedSubjects = new LinkedList<>();
            Iterator<String> subjectIterator = action.getSubjects().iterator();
            while (subjectIterator.hasNext()) {
                sortedSubjects.add(subjectIterator.next());
            }
            // Manual sort instead of Collections.sort
            this.sortStringList(sortedSubjects);
            Iterator<String> sortedSubjectIterator = sortedSubjects.iterator();
            while (sortedSubjectIterator.hasNext()) {
                String subject = sortedSubjectIterator.next();
                keyBuilder.append("S:");
                keyBuilder.append(subject);
                keyBuilder.append(";");
            }

            // Add consumed entities to key
            List<String> sortedConsumed = new LinkedList<>();
            Iterator<String> consumedIterator = action.getConsumed().iterator();
            while (consumedIterator.hasNext()) {
                sortedConsumed.add(consumedIterator.next());
            }
            // Manual sort
            this.sortStringList(sortedConsumed);
            Iterator<String> sortedConsumedIterator = sortedConsumed.iterator();
            while (sortedConsumedIterator.hasNext()) {
                String entity = sortedConsumedIterator.next();
                keyBuilder.append("C:");
                keyBuilder.append(entity);
                keyBuilder.append(";");
            }

            // Add produced entities to key
            List<String> sortedProduced = new LinkedList<>();
            Iterator<String> producedIterator = action.getProduced().iterator();
            while (producedIterator.hasNext()) {
                sortedProduced.add(producedIterator.next());
            }
            // Manual sort
            this.sortStringList(sortedProduced);
            Iterator<String> sortedProducedIterator = sortedProduced.iterator();
            while (sortedProducedIterator.hasNext()) {
                String entity = sortedProducedIterator.next();
                keyBuilder.append("P:");
                keyBuilder.append(entity);
                keyBuilder.append(";");
            }

            String actionKey = keyBuilder.toString();

            if (!actionGroups.containsKey(actionKey)) {
                actionGroups.put(actionKey, new LinkedList<GameAction>());
            }
            actionGroups.get(actionKey).add(action);
        }

        // If there are multiple action groups, it's ambiguous
        if (actionGroups.size() > 1) {
            return "Ambiguous command. Your command contains triggers for different actions.";
        }

        // Now we know all candidates are for the same action, choose the one with the longest matching trigger
        GameAction bestAction = null;
        String bestTrigger = null;
        int longestTriggerLength = 0;

        Iterator<GameAction> candidateIterator = candidateActions.iterator();
        while (candidateIterator.hasNext()) {
            GameAction action = candidateIterator.next();
            String matchingTrigger = action.getMatchingTrigger(fullCommand);
            if (matchingTrigger != null) {
                if (bestTrigger == null || matchingTrigger.length() > longestTriggerLength) {
                    bestAction = action;
                    bestTrigger = matchingTrigger;
                    longestTriggerLength = matchingTrigger.length();
                }
            }
        }

        if (bestAction == null) {
            return "I don't understand what you want to do.";
        }

        System.out.println("Selected best action with trigger: " + bestTrigger);

        // Check if at least one subject is mentioned
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

        // Check if ALL required subject entities are available to the player
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

        // Execute the matching action
        return this.performAction(player, bestAction);
    }

    /**
     * Simple method to sort a list of strings without using Collections.sort
     */
    private void sortStringList(List<String> list) {
        // Simple bubble sort implementation
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    // Swap elements
                    String temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * Check if an entity is available to the player (in inventory or current location)
     */
    private boolean isEntityAvailableToPlayer(Player player, String entityName) {
        Location currentLocation = player.getCurrentLocation();

        // Check player inventory
        if (player.getFromInventory(entityName) != null) {
            System.out.println("Entity '" + entityName + "' found in player inventory");
            return true;
        }

        // Check artefacts in current location
        if (currentLocation.getArtefact(entityName) != null) {
            System.out.println("Entity '" + entityName + "' found as artefact in current location");
            return true;
        }

        // Check furniture in current location
        for (Furniture furniture : currentLocation.getFurniture()) {
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                System.out.println("Entity '" + entityName + "' found as furniture in current location");
                return true;
            }
        }

        // Check characters in current location
        for (Character character : currentLocation.getCharacters()) {
            if (character.getName().equalsIgnoreCase(entityName)) {
                System.out.println("Entity '" + entityName + "' found as character in current location");
                return true;
            }
        }

        // Check if the location itself is a subject
        if (currentLocation.getName().equalsIgnoreCase(entityName)) {
            System.out.println("Entity '" + entityName + "' is the current location");
            return true;
        }

        System.out.println("Entity '" + entityName + "' is NOT available to player");
        return false;
    }

    /**
     * Check if a word is a common word that should be ignored
     */
    private boolean isCommonWord(String word) {
        // List of common words to ignore
        Set<String> commonWords = new HashSet<>();
        commonWords.add("the");
        commonWords.add("a");
        commonWords.add("an");
        commonWords.add("and");
        commonWords.add("with");
        commonWords.add("using");
        commonWords.add("to");
        commonWords.add("at");
        commonWords.add("in");
        commonWords.add("on");
        commonWords.add("by");
        commonWords.add("for");
        commonWords.add("from");
        commonWords.add("of");
        commonWords.add("or");

        return commonWords.contains(word.toLowerCase());
    }

    // We no longer need the isCommonWord method as we now look for matches directly


    private String performAction(Player player, GameAction action) {
        Location currentLocation = player.getCurrentLocation();

        // Note: Subject entity availability is now checked in executeCustomAction
        // We can assume all required entities are available at this point

        // Handle consumed entities
        for (String entityName : action.getConsumed()) {
            if (entityName.equalsIgnoreCase("health")) {
                player.decreaseHealth();
                if (player.isDead()) {
                    // Immediately reset the player and move to start location
                    player.resetHealth();
                    player.setCurrentLocation(this.gameWorld.getStartLocation());
                    return action.getNarration() + "\nYou died and lost all of your items, you must return to the start of the game";
                }
                continue;
            }

            // Check if it's a location (path removal)
            Location locationToConsume = this.gameWorld.getLocation(entityName);
            if (locationToConsume != null) {
                GamePath pathToRemove = currentLocation.getPathTo(locationToConsume);
                if (pathToRemove != null) {
                    currentLocation.removePath(pathToRemove);
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
                GamePath newPath = new GamePath(currentLocation, locationToProduce);
                currentLocation.addPath(newPath);
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
                    this.blockingHandleConnection(s);
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
                String result = this.handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}