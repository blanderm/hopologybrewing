package com.hopologybrewing.bcs.capture.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import com.hopologybrewing.bcs.capture.service.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ddcbryanl on 4/14/17.
 */
public class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private TemperatureProbeMessageRecorder tempRecorder;
    private OutputMessageRecorder outuputRecorder;
    private DbService dbService;
    private String tempFileLocation;
    private String outputFileLocation;

    public void loadData() {
        // read data from file and call recorder to save to DynamoDB
        // see TemperatureService and OutputService for reading data from file
        List<BrewInfo> brews = dbService.getAllBrews();
        int index = BrewInfo.getMostRecentBrewIndex(brews);

        if (index >= 0) {
            BrewInfo brew = brews.get(index);

            // loads data into a map based on timestamp
            Map<Long, List<TemperatureProbeRecording>> tempMap = loadTemperatureData(brew);

            for (List<TemperatureProbeRecording> list : tempMap.values()) {
                // record values for the timestamp
                tempRecorder.recordMessage(list);
            }

            // loads data into a map based on timestamp
            Map<Long, List<OutputRecording>> outputMap = loadOutputData(brew);

            for (List<OutputRecording> list : outputMap.values()) {
                // record values for the timestamp
                outuputRecorder.recordMessage(list);
            }
        } else {
            log.error("Couldn't find most recent brew.");
        }


    }

    private Map<Long, List<OutputRecording>> loadOutputData(BrewInfo brew) {
        String line;
        BufferedReader reader = null;
        Map<Long, List<OutputRecording>> map = new HashMap<>();
        List<OutputRecording> list = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            OutputRecording outputRecording = null;
            reader = new BufferedReader(new FileReader(outputFileLocation));

            try {
                while ((line = reader.readLine()) != null) {
                    outputRecording = mapper.readValue(line, OutputRecording.class);

                    if (outputRecording != null && outputRecording.getData() != null && outputRecording.getTimestamp().getTime() >= brew.getBrewDate()
                            && (brew.getFermentationComplete() == 0 || outputRecording.getTimestamp().getTime() <= brew.getFermentationComplete())) {

                        // set brew date
                        outputRecording.setBrewDate(brew.getBrewDateAsDate());

                        // put in map by timestamp
                        list = map.get(outputRecording.getTimestamp().getTime());

                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.add(outputRecording);
                        map.put(outputRecording.getTimestamp().getTime(), list);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file " + outputFileLocation + " - ", e);
            }
        } catch (FileNotFoundException e) {
            log.error("File not found for " + outputFileLocation + " - ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close reader for " + outputFileLocation + " - ", e);
                }
            }
        }

        return map;
    }

    private Map<Long, List<TemperatureProbeRecording>> loadTemperatureData(BrewInfo brew) {
        String line;
        BufferedReader reader = null;
        Map<Long, List<TemperatureProbeRecording>> map = new HashMap<>();
        List<TemperatureProbeRecording> list = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            TemperatureProbeRecording probeRecording = null;
            reader = new BufferedReader(new FileReader(tempFileLocation));

            try {
                while ((line = reader.readLine()) != null) {
                    probeRecording = mapper.readValue(line, TemperatureProbeRecording.class);

                    if (probeRecording != null && probeRecording.getData() != null && probeRecording.getTimestamp().getTime() >= brew.getBrewDate()
                            && (brew.getFermentationComplete() == 0 || probeRecording.getTimestamp().getTime() <= brew.getFermentationComplete())) {
                        // set brew date
                        probeRecording.setBrewDate(brew.getBrewDateAsDate());

                        // put in map by timestamp
                        list = map.get(probeRecording.getTimestamp().getTime());

                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        list.add(probeRecording);
                        map.put(probeRecording.getTimestamp().getTime(), list);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file " + tempFileLocation + " - ", e);
            }
        } catch (FileNotFoundException e) {
            log.error("File not found for " + tempFileLocation + " - ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close reader for " + tempFileLocation + " - ", e);
                }
            }
        }

        return map;
    }

    @Autowired
    public void setTempRecorder(TemperatureProbeMessageRecorder tempRecorder) {
        this.tempRecorder = tempRecorder;
    }

    @Autowired
    public void setOutuputRecorder(OutputMessageRecorder outuputRecorder) {
        this.outuputRecorder = outuputRecorder;
    }

    @Autowired
    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }

    public String getTempFileLocation() {
        return tempFileLocation;
    }

    public void setTempFileLocation(String tempFileLocation) {
        this.tempFileLocation = tempFileLocation;
    }

    public String getOutputFileLocation() {
        return outputFileLocation;
    }

    public void setOutputFileLocation(String outputFileLocation) {
        this.outputFileLocation = outputFileLocation;
    }
}
