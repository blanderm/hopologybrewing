package com.hopologybrewing.bcs.capture.model;

import java.util.Date;

/**
 * Created by ddcbryanl on 12/13/16.
 */
public class TemperatureProbeRecording extends Recording {
    private TemperatureProbe data;
    public static final String TEMPERATURE_TYPE = "temperature_recording";

    public TemperatureProbeRecording() {
    }

    public TemperatureProbeRecording(TemperatureProbe probe, Date timestamp) {
        this.data = probe;
        this.timestamp = timestamp;
    }

    public TemperatureProbeRecording(TemperatureProbe probe, Date timestamp, Date brewDate) {
        this.data = probe;
        this.timestamp = timestamp;
        this.setBrewDate(brewDate);
    }

    @Override
    public TemperatureProbe getData() {
        return data;
    }

    public void setData(TemperatureProbe data) {
        this.data = data;
    }

    @Override
    public String getName() {
        String name = null;
        if (data != null) {
            name = data.getName();
        }

        return name;
    }

    @Override
    public String getType() {
        return TEMPERATURE_TYPE;
    }

    public void setProbe(TemperatureProbe probe) {
        this.data = probe;
    }
}
