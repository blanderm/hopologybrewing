package com.hopologybrewing.bcs.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Output;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.Recording;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by ddcbryanl on 12/14/16.
 */
public class OutputService extends BcsService {
    private static final Logger log = LoggerFactory.getLogger(OutputService.class);
    private DbService dbService;

    public OutputService() {
    }

    public OutputService(String user, String pwd) {
        super(user, pwd);
    }

    public OutputService(String user, String pwd, String ip) {
        super(user, pwd, ip);
    }

    public Output getOutput(String outputId) {
        return (Output) super.getData(BcsService.Type.OUTPUT, outputId);
    }

    public List<Output> getEnabledOutputs() {
        List<Output> enabledOutputs = new ArrayList<Output>();
        List outputs = (List) super.getData(BcsService.Type.OUTPUTS);

        if (outputs != null) {
            for (int i = 0; i < outputs.size(); i++) {
                if (outputs.get(i) != null) {
                    enabledOutputs.add(getOutput(String.valueOf(i)));
                }
            }
        }

        return enabledOutputs;
    }

    public Map<String, List<List>> getProbeDataForBrew(long lowerRange, long upperRange) {
        Date date = null;
        try {
            date = dbService.getMostRecentBrewDate();
        } catch (ExecutionException e) {
            log.error("Failed to find current brew date - ", e);
        }

        return getProbeDataForBrew(date, lowerRange, upperRange);
    }

    public Map<String, List<List>> getProbeDataForBrew(Date brewDate, long lowerRange, long upperRange) {
        List data = null;
        List<List> recordingDataList = null;
        OutputRecording outputRecording = null;
        Map<String, List<List>> outputsMap = new HashMap<>();
        List<Recording> recordings = dbService.findOutputReadings(brewDate, lowerRange, upperRange);

        for (Recording recording : recordings) {
            if (recording instanceof OutputRecording) {
                outputRecording = (OutputRecording) recording;
                recordingDataList = outputsMap.get(outputRecording.getName());

                if (recordingDataList == null) {
                    recordingDataList = new ArrayList<>();
                }

                data = new ArrayList<>();
                data.add(outputRecording.getTimestamp());
                data.add((outputRecording.getData().isOn() ? 1 : 0));

                recordingDataList.add(data);
                outputsMap.put(outputRecording.getName(), recordingDataList);
            }
        }

        return outputsMap;
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}


