package com.hopologybrewing.bcs.capture.model;

/**
 * Created by ddcbryanl on 12/20/16.
 */
public class CurrentState {
    private Integer state;
    private boolean waiting;

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
