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
    private String fileLocation;
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

    public Map<String, List<List>> getHistoricalOutputData(long lower, long upper, int limit) {
        List data = null;
        List<List> recordingDataList = null;
        List<Recording> recordingList = null;
        OutputRecording outputRecording = null;
        Map<String, List<List>> outputsMap = new HashMap<>();

        List<Output> outputs = getEnabledOutputs();
        for (Output output : outputs) {
            // todo: fix it
//            recordingList = dbService.queryRecording(OutputRecording.class, output.getName(), lower, upper, limit, false);

            for (Recording recording : recordingList) {
                if (recording instanceof OutputRecording) {
                    outputRecording = (OutputRecording) recording;
                    recordingDataList = outputsMap.get(output.getName());

                    if (recordingDataList == null) {
                        recordingDataList = new ArrayList<>();
                    }

                    data = new ArrayList<>();
                    data.add(outputRecording.getTimestamp());
                    data.add((outputRecording.getData().isOn() ? 1 : 0));

                    recordingDataList.add(data);
                    outputsMap.put(output.getName(), recordingDataList);
                }
            }
        }

        return outputsMap;
    }

    public Map<String, List<List>> getProbeDataForBrew() {
        Date date = null;
        try {
            date = dbService.getCurrentBrewDate();
        } catch (ExecutionException e) {
            log.error("Failed to find current brew date - ", e);
        }

        return getProbeDataForBrew(date);
    }

    public Map<String, List<List>> getProbeDataForBrew(Date brewDate) {
        List data = null;
        List<List> recordingDataList = null;
        OutputRecording outputRecording = null;
        Map<String, List<List>> outputsMap = new HashMap<>();
        List<Recording> recordings = dbService.findOutputReadings(brewDate);

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

    public Map<String, List<List>> getHistoricalOutputDataFromFile() {
        String line;
        List data = null;
        List<List> recordings = null;
        ReversedLinesFileReader reader = null;
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<List>> outputsMap = new HashMap<>();

        try {
            OutputRecording outputRecording = null;
            int numLines = 60 * 60 * 24 * 7;
            int counter = 0;

            reader = new ReversedLinesFileReader(new File(fileLocation));

            while ((line = reader.readLine()) != null && counter < numLines) {
                outputRecording = mapper.readValue(line, OutputRecording.class);
                if (outputRecording != null && outputRecording.getData() != null) {
                    recordings = outputsMap.get(outputRecording.getData().getName());

                    if (recordings == null) {
                        recordings = new ArrayList<>();
                    }

                    data = new ArrayList<>();
                    data.add(outputRecording.getTimestamp());
                    data.add((outputRecording.getData().isOn() ? 1 : 0));

                    recordings.add(data);
                    outputsMap.put(outputRecording.getData().getName(), recordings);
                }

                counter++;
            }
        } catch (IOException e) {
            log.error("Error reading file " + fileLocation + " - ", e);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close reader for " + fileLocation + " - ", e);
                }
            }
        }

        return outputsMap;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}


