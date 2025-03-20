package edu.uob;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

public class GameActionManager {
    private Set<GameAction> actions;

    public GameActionManager() {
        this.actions = new HashSet<>();
    }

    public void addAction(GameAction action) {
        this.actions.add(action);
    }

    public Set<GameAction> getAllActions() {
        return new HashSet<>(this.actions);
    }

    /**
     * Find all actions that match a given command string
     */
    public List<GameAction> findActionsByTrigger(String command) {
        List<GameAction> matchingActions = new LinkedList<>();
        String lowerCommand = command.toLowerCase();

        Iterator<GameAction> actionIterator = this.actions.iterator();
        while (actionIterator.hasNext()) {
            GameAction action = actionIterator.next();
            if (action.hasTrigger(lowerCommand)) {
                matchingActions.add(action);
            }
        }

        return matchingActions;
    }
}