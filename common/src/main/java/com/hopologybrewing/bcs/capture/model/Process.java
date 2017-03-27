package com.hopologybrewing.bcs.capture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ddcbryanl on 12/5/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Process {
    private List<String> states;
    private List<State> statesObj = new ArrayList<State>();
    private String name;
    private boolean running;
    private boolean paused;
    @JsonProperty("run_on_startup")
    private boolean runOnStartup;
    @JsonProperty("current_state")
    private CurrentState currentState;
    private boolean display;

    public List<String> getStates() {
        return states;
    }

    public void setStates(List<String> states) {
        this.states = states;

        // todo: decide if it's ok to add to an existing list, if existing should be overwritten, etc.
        if (states != null && statesObj.isEmpty()) {
            State st = null;
            for (String s : states) {
                st = new State(s);
                statesObj.add(st);
            }
        }
    }

    public List<State> getStatesObj() {
        return statesObj;
    }

    public void setStatesObj(List<State> statesObj) {
        this.statesObj = statesObj;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public CurrentState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CurrentState currentState) {
        this.currentState = currentState;
    }
}
