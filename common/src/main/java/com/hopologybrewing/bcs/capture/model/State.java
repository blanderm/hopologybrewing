package com.hopologybrewing.bcs.capture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.*;
import java.util.List;

/**
 * Created by ddcbryanl on 12/5/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class State {
    private String name;
    private List<Timer> timers;
    private Ramp ramp;
    private List<Boolean> outputs;
    @JsonProperty("process_spawn")
    private List<Boolean> processSpawn;
    @JsonProperty("process_kill")
    private List<Boolean> processKill;
    @JsonProperty("state_alarm")
    private int stateAlarm;
    @JsonProperty("email_alarm")
    private boolean emailAlarm;
    private List<Boolean> registers;
    private List<ExitCondition> exitConditions;

    public State() {}

    public State(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Timer> getTimers() {
        return timers;
    }

    public void setTimers(List<Timer> timers) {
        this.timers = timers;
    }

    public Ramp getRamp() {
        return ramp;
    }

    public void setRamp(Ramp ramp) {
        this.ramp = ramp;
    }

    public int getStateAlarm() {
        return stateAlarm;
    }

    public void setStateAlarm(int stateAlarm) {
        this.stateAlarm = stateAlarm;
    }

    public boolean isEmailAlarm() {
        return emailAlarm;
    }

    public void setEmailAlarm(boolean emailAlarm) {
        this.emailAlarm = emailAlarm;
    }

    public List<Boolean> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Boolean> outputs) {
        this.outputs = outputs;
    }

    public List<Boolean> getProcessSpawn() {
        return processSpawn;
    }

    public void setProcessSpawn(List<Boolean> processSpawn) {
        this.processSpawn = processSpawn;
    }

    public List<Boolean> getProcessKill() {
        return processKill;
    }

    public void setProcessKill(List<Boolean> processKill) {
        this.processKill = processKill;
    }

    public List<Boolean> getRegisters() {
        return registers;
    }

    public void setRegisters(List<Boolean> registers) {
        this.registers = registers;
    }

    public List<ExitCondition> getExitConditions() {
        return exitConditions;
    }

    public void setExitConditions(List<ExitCondition> exitConditions) {
        this.exitConditions = exitConditions;
    }
}
