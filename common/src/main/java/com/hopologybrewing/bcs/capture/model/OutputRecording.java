package com.hopologybrewing.bcs.capture.model;

import java.util.Date;

public class OutputRecording extends Recording {
    private Output data;

    public OutputRecording() {
    }

    public OutputRecording(Output o, Date timestamp) {
        this.data = o;
        this.timestamp = timestamp;
    }

    @Override
    public Output getData() {
        return data;
    }

    public void setData(Output data) {
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
