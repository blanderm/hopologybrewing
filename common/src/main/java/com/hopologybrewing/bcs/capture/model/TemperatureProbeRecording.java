package com.hopologybrewing.bcs.capture.model;

import java.util.Date;

/**
 * Created by ddcbryanl on 12/13/16.
 */
public class TemperatureProbeRecording extends Recording {
    private TemperatureProbe data;

    public TemperatureProbeRecording() {
    }

    public TemperatureProbeRecording(TemperatureProbe probe, Date timestamp) {
        this.data = probe;
        this.timestamp = timestamp;
    }

    @Override
    public TemperatureProbe getData() {
        return data;
    }

    public void setData(TemperatureProbe data) {
        this.data = data;
    }

    @Override
    protected String getName() {
        String name = null;
        if (data != null) {
            name = data.getName();
        }

        return name;
    }
}
