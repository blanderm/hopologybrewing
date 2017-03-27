package com.hopologybrewing.bcs.capture.batch;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Output;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.service.DbService;
import com.hopologybrewing.bcs.capture.service.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OutputMessageRecorder {
    // {"output":{"name":"Pump - Left","on":true,"enabled":true},"timestamp":1484761482879}
    //{"output":{"name":"Pump - Right","on":true,"enabled":true},"timestamp":1484761482879}
    private static final Logger log = LoggerFactory.getLogger(OutputMessageRecorder.class);
    private static final Logger historyLogger = LoggerFactory.getLogger("bcs-outputs-history");
    private OutputService outputService;
    private DbService dbService;

    public List<OutputRecording> getNextOutputReading() {
        Date date = new Date();
        List<Output> outputs = outputService.getEnabledOutputs();
        List<OutputRecording> recordings = new ArrayList<>();

        if (outputs != null && !outputs.isEmpty()) {
            for (Output output : outputs) {
                recordings.add(new OutputRecording(output, date));
            }
        }

        return recordings;
    }

    public void recordMessage(List<OutputRecording> message) {
        if (message != null && message.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            for (OutputRecording recording : message) {
                try {
                    historyLogger.info(mapper.writeValueAsString(message) + "\n");
                } catch (JsonProcessingException e) {
                    log.error("Failed creating json ", e);
                }

                // put message in DynamoDB
                dbService.writeRecording(recording);
            }
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
