package com.hopologybrewing.bcs.capture.batch;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by ddcbryanl on 4/14/17.
 */
public class DataLoader {
    private TemperatureProbeMessageRecorder tempRecorder;
    private OutputMessageRecorder outuputRecorder;

    public void loadData() {
        // read data from file and call recorder to save to DynamoDB
        // see TemperatureService and OutputService for reading data from file
    }

    @Autowired
    public void setTempRecorder(TemperatureProbeMessageRecorder tempRecorder) {
        this.tempRecorder = tempRecorder;
    }

    @Autowired
    public void setOutuputRecorder(OutputMessageRecorder outuputRecorder) {
        this.outuputRecorder = outuputRecorder;
    }
}
