package com.hopologybrewing.bcs.capture.aws.lambda;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.batch.TemperatureProbeMessageRecorder;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import com.hopologybrewing.bcs.capture.service.TemperatureService;
import com.amazonaws.services.lambda.runtime.Context;

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
        recorder.setDbService(dynamoDB);
        recorder.setTempService(tempSvc);

        logger.log("Getting next temperature recordings.");
        List<TemperatureProbeRecording> recordings = recorder.getNextTemperatureReading();
        for (TemperatureProbeRecording r : recordings) {
            logger.log(String.format("Recording for %s at %s : temp is %s (%s SP)", r.getId(), r.getTimestamp().toString(), r.getData().getTemp(), r.getData().getSetpoint()));
        }

        logger.log("Record temperature recordings...");
        recorder.recordMessage(recordings);
        logger.log("TemperaturePoller : end");
    }
}
