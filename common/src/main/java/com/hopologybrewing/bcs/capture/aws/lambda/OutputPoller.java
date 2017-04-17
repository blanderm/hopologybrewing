package com.hopologybrewing.bcs.capture.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.batch.OutputMessageRecorder;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.service.DbService;
import com.hopologybrewing.bcs.capture.service.OutputService;

import java.util.List;

/**
 * Created by ddcbryanl on 3/17/17.
 */
public class OutputPoller extends Poller {
    public void poll(String event, Context context) {
        LambdaLogger logger = context.getLogger();
        DbService dynamoDB = new DynamoDBService();

        OutputService outSvc = new OutputService(DECRYPTED_UNAME, DECRYPTED_PASS, DECRYPTED_IP);
        outSvc.setFileLocation("logs/bcs-temps.log");
        outSvc.setDbService(dynamoDB);

        OutputMessageRecorder recorder = new OutputMessageRecorder();
        recorder.setDbService(dynamoDB);
        recorder.setOutputService(outSvc);

        List<OutputRecording> recordings = recorder.getNextOutputReading();
        recorder.recordMessage(recordings);

        for (OutputRecording r : recordings) {
            logger.log(String.format("Recording for %s at %s : %s is on? %s", r.getTimestamp().toString(), r.getData().getName(), r.getData().isOn()));
        }

    }
}
