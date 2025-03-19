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

    public boolean hasTrigger(String trigger) {
        return this.triggers.contains(trigger.toLowerCase());
    }
}