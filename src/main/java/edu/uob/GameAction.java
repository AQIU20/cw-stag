package edu.uob;

import java.util.HashSet;
import java.util.Set;

public class GameAction {
    private Set<String> triggers;
    private Set<String> subjects;
    private Set<String> consumed;
    private Set<String> produced;
    private String narration;

    public GameAction() {
        this.triggers = new HashSet<>();
        this.subjects = new HashSet<>();
        this.consumed = new HashSet<>();
        this.produced = new HashSet<>();
        this.narration = "";
    }

    public void addTrigger(String trigger) {
        this.triggers.add(trigger.toLowerCase());
    }

    public void addSubject(String subject) {
        this.subjects.add(subject.toLowerCase());
    }

    public void addConsumed(String entity) {
        this.consumed.add(entity.toLowerCase());
    }

    public void addProduced(String entity) {
        this.produced.add(entity.toLowerCase());
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public Set<String> getTriggers() {
        return new HashSet<>(this.triggers);
    }

    public Set<String> getSubjects() {
        return new HashSet<>(this.subjects);
    }

    public Set<String> getConsumed() {
        return new HashSet<>(this.consumed);
    }

    public Set<String> getProduced() {
        return new HashSet<>(this.produced);
    }

    public String getNarration() {
        return this.narration;
    }

    /**
     * Check if this action's trigger is contained in the command.
     * Returns the matched trigger if found, otherwise null.
     */
    public String getMatchingTrigger(String command) {
        String longestMatch = null;
        int longestLength = 0;

        for (String trigger : this.triggers) {
            if (command.contains(trigger)) {
                // Keep track of the longest matching trigger
                if (trigger.length() > longestLength) {
                    longestMatch = trigger;
                    longestLength = trigger.length();
                }
            }
        }

        return longestMatch;
    }

    /**
     * Check if any trigger matches the command
     */
    public boolean hasTrigger(String command) {
        return getMatchingTrigger(command) != null;
    }
}