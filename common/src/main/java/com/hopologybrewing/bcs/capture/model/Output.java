package com.hopologybrewing.bcs.capture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by ddcbryanl on 12/5/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Output {
    private String name;
    private boolean on;
    private boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
