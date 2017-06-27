package com.hopologybrewing.bcs.capture.batch;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.Output;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.service.DbService;
import com.hopologybrewing.bcs.capture.service.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OutputMessageRecorder {
    private static final Logger log = LoggerFactory.getLogger(OutputMessageRecorder.class);
    private static final Logger historyLogger = LoggerFactory.getLogger("bcs-outputs-history");
    private OutputService outputService;
    private DbService dbService;

    public List<OutputRecording> getNextOutputReading() {
        List<OutputRecording> recordings = new ArrayList<>();

        try {
            // only record data if there is an active brew
            BrewInfo info = dbService.getCurrentBrew();
            if (info != null && info.getCrashStart() == 0L) {
                Date date = new Date();
                List<Output> outputs = outputService.getEnabledOutputs();

                boolean atLeastOneOn = false;
                if (outputs != null && !outputs.isEmpty()) {
                    for (Output output : outputs) {
                        if (output.isOn()) {
                            atLeastOneOn = true;
                        }

                        recordings.add(new OutputRecording(output, date));
                    }
                }

                // drop data if all outputs are off
                if (!atLeastOneOn) {
                    return new ArrayList<>();
                }
            } else {
                log.info("There may not be an active brew or crashing has begun.  Data will not be collected for this cycle");
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

    public void recordMessage(List<OutputRecording> message) {
        if (message != null && message.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            List<Recording> recordings = new ArrayList<>();
            for (OutputRecording recording : message) {
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
    public void setOutputService(OutputService outputService) {
        this.outputService = outputService;
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}
