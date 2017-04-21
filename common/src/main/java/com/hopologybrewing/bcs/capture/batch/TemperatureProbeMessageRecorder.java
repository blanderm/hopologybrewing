package com.hopologybrewing.bcs.capture.batch;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbe;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import com.hopologybrewing.bcs.capture.service.DbService;
import com.hopologybrewing.bcs.capture.service.TemperatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TemperatureProbeMessageRecorder {
    private static final Logger log = LoggerFactory.getLogger(TemperatureProbeMessageRecorder.class);
    private static final Logger historyLogger = LoggerFactory.getLogger("bcs-temps-history");
    private TemperatureService tempService;
    private DbService dbService;

    public List<TemperatureProbeRecording> getNextTemperatureReading() {
        List<TemperatureProbeRecording> recordings = new ArrayList<>();

        try {
            if (dbService.getCurrentBrewDate() != null) {
                Date date = new Date();
                List<TemperatureProbe> probes = tempService.getEnabledProbes();

                if (probes != null && !probes.isEmpty()) {

                    for (TemperatureProbe probe : probes) {
                        recordings.add(new TemperatureProbeRecording(probe, date));
                    }
                }
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UTFDataFormatException) {
                // no need to fill up the log if there isn't an active brew
                log.debug("There may not be an active brew or a failure occured determining the active brew.  Data will not be collected for this cycle", e);
            } else {
                log.error("Failed loading current brew date - ", e);
            }
        }

        return recordings;
    }

    public void recordMessage(List<TemperatureProbeRecording> message) {
        if (message != null && message.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();

            List<Recording> recordings = new ArrayList<>();
            for (TemperatureProbeRecording recording : message) {
                try {
                    historyLogger.info(mapper.writeValueAsString(message) + "\n");
                    recordings.add(recording);
                } catch (JsonProcessingException e) {
                    log.error("Failed creating json ", e);
                }
            }

            // put message in DynamoDB
            dbService.saveReadings(recordings);
        }
    }

    @Autowired
    public void setTempService(TemperatureService tempService) {
        this.tempService = tempService;
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}
