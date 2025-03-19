package edu.uob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameActionManager {
    private List<GameAction> actions;

    public GameActionManager() {
        this.actions = new ArrayList<>();
    }

    public void addAction(GameAction action) {
        this.actions.add(action);
    }

    public List<GameAction> getAllActions() {
        return new ArrayList<>(this.actions);
    }

    /**
     * Find all actions that match a given trigger phrase
     */
    public List<GameAction> findActionsByTrigger(String trigger) {
        List<GameAction> matchingActions = new ArrayList<>();
        String lowerTrigger = trigger.toLowerCase();

        for (GameAction action : this.actions) {
            if (action.hasTrigger(lowerTrigger)) {
                matchingActions.add(action);
            }
        }

        return matchingActions;
    }

    /**
     * Find all actions that match the given trigger and subject entities
     */
    public List<GameAction> findMatchingActions(String trigger, Set<String> subjects) {
        List<GameAction> triggeredActions = this.findActionsByTrigger(trigger);
        List<GameAction> matchingActions = new ArrayList<>();

        // Debug: Print information about matching process
        System.out.println("Finding actions for trigger: " + trigger);
        System.out.println("Available subjects: " + subjects);

        for (GameAction action : triggeredActions) {
            // Check if all required subject entities are provided
            Set<String> requiredSubjects = action.getSubjects();
            Set<String> providedSubjects = new HashSet<>();

            for (String subject : subjects) {
                providedSubjects.add(subject.toLowerCase());
            }

            // Debug: Print required subjects for this action
            System.out.println("Action requires subjects: " + requiredSubjects);
            System.out.println("Command provides subjects: " + providedSubjects);

            // Action is valid if all required subjects are provided
            boolean allRequiredSubjectsProvided = providedSubjects.containsAll(requiredSubjects);

            // For now, don't enforce the "no extraneous subjects" constraint
            // This allows partial matching with additional words

            System.out.println("All required subjects provided? " + allRequiredSubjectsProvided);

            if (allRequiredSubjectsProvided) {
                matchingActions.add(action);
                System.out.println("Action matched!");
            }
        }

        return matchingActions;
    }
}

