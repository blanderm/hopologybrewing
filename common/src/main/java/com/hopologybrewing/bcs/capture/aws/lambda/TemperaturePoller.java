package com.hopologybrewing.bcs.capture.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBServiceOld;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.batch.TemperatureProbeMessageRecorder;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import com.hopologybrewing.bcs.capture.service.TemperatureService;

import java.util.List;

/**
 * Created by ddcbryanl on 3/17/17.
 */
public class TemperaturePoller extends Poller {
    public void poll(String event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("TemperaturePoller : start");
        DynamoDBService dynamoDB = new DynamoDBService();

        TemperatureService tempSvc = new TemperatureService(DECRYPTED_UNAME, DECRYPTED_PASS, DECRYPTED_IP);
        tempSvc.setFileLocation("logs/bcs-temps.log");
        tempSvc.setDbService(dynamoDB);

        TemperatureProbeMessageRecorder recorder = new TemperatureProbeMessageRecorder();
//        recorder.setDbService(dynamoDB);
        recorder.setTempService(tempSvc);

        logger.log("Getting next temperature recordings.");
        List<TemperatureProbeRecording> recordings = recorder.getNextTemperatureReading();

//        for (TemperatureProbeRecording r : recordings) {
//            logger.log(String.format("Recording for %s at %s : temp is %s (%s SP)", r.getId(), r.getTimestamp().toString(), r.getData().getTemp(), r.getData().getSetpoint()));
//        }

        logger.log("Record temperature recordings...");
//        recordData(dynamoDB, recordings);
        logger.log("TemperaturePoller : end");
    }

    protected void recordData(DynamoDBServiceOld dynamoDB, List<TemperatureProbeRecording> message) {
        if (message != null && message.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();

            for (Recording recording : message) {
                try {
                    System.out.println(mapper.writeValueAsString(message) + "\n");
                } catch (JsonProcessingException e) {
                    System.out.println("Failed creating json ");
                    e.printStackTrace();
                }

                // todo: put message in DynamoDB
            }
        }
    }
}
