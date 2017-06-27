package com.hopologybrewing.bcs.capture.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import com.hopologybrewing.bcs.capture.service.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        List<BrewInfo> brews = null;
        try {
            brews = dbService.getAllBrews();
        } catch (ExecutionException e) {
            log.error("Failed to load all brews - ", e);
            brews = new ArrayList<>();
        }

        int index = BrewInfo.getMostRecentBrewIndex(brews);

        if (index >= 0) {
            BrewInfo brew = brews.get(index);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    // loads data into a map based on timestamp
                    Map<Long, List<TemperatureProbeRecording>> tempMap = loadTemperatureData(brew);

                    for (List<TemperatureProbeRecording> list : tempMap.values()) {

                        // record values for the timestamp
                        tempRecorder.recordMessage(list);
                    }
                }
            });

            executor.submit(new Runnable() {
                @Override
                public void run() {

                    // loads data into a map based on timestamp
                    Map<Long, List<OutputRecording>> outputMap = loadOutputData(brew);

                    for (List<OutputRecording> list : outputMap.values()) {
                        // record values for the timestamp
                        outuputRecorder.recordMessage(list);
                    }
                }
            });

            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            log.error("Couldn't find most recent brew.");
        }
    }

    private Map<Long, List<OutputRecording>> loadOutputData(BrewInfo brew) {
        String line;
        BufferedReader reader = null;
        Map<Long, List<OutputRecording>> map = new LinkedHashMap<>();

        try {
            reader = new BufferedReader(new FileReader(outputFileLocation));

            try {
                String[] split;
                String newLine;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("[") && line.endsWith("]")) {
                        // todo: split into two lines and process both
                        split = StringUtils.split(line, "},{");
                        for (int i = 0; i < split.length; i++) {
                            newLine = split[i].replace("[{", "{");
                            newLine = newLine.replace("}]", "}");

                            if (!newLine.startsWith("{")) {
                                newLine = "{" + newLine;
                            }

                            if (!newLine.endsWith("}")) {
                                newLine = newLine + "}";
                            }

                            processLine(brew, newLine, map);
                        }
                    } else {
                        processLine(brew, line, map);
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
                    log.error("Failed to close reader for " + outputFileLocation + " - " + e.getMessage());
                }
            }
        }

        // remove entries where all outputs were off which can't be determined until all data has been processed
        Map<Long, List<OutputRecording>> finalMap = new LinkedHashMap<>();

        if (map.size() > 0) {
            for (Map.Entry<Long, List<OutputRecording>> entry : map.entrySet()) {
                for (OutputRecording recording : entry.getValue()) {
                    if (recording.getData().isOn()) {
                        finalMap.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
        }

        return finalMap;
    }

    private void processLine(BrewInfo brew, String line, Map<Long, List<OutputRecording>> map) {
        List<OutputRecording> list = null;
        ObjectMapper mapper = new ObjectMapper();
        OutputRecording outputRecording = null;

        try {
            outputRecording = mapper.readValue(line, OutputRecording.class);

            if (outputRecording != null && outputRecording.getData() != null && outputRecording.getTimestamp().getTime() >= brew.getBrewDate()
                    && (brew.getBrewCompleteDate() == 0 || outputRecording.getTimestamp().getTime() <= brew.getBrewCompleteDate())) {

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
        } catch (IOException e) {
            log.error("Error parsing json from " + outputFileLocation + ", dropping data point - ", e);
        }
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
                int droppedDataPoints = 0;
                while ((line = reader.readLine()) != null) {
                    try {
                        probeRecording = mapper.readValue(line, TemperatureProbeRecording.class);

                        if (probeRecording != null && probeRecording.getData() != null && probeRecording.getTimestamp().getTime() >= brew.getBrewDate()
                                && (brew.getBrewCompleteDate() == 0 || probeRecording.getTimestamp().getTime() <= brew.getBrewCompleteDate())) {
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
                    } catch (IOException e) {
                        log.error("Error parsing json from " + tempFileLocation + ", dropping data point #" + ++droppedDataPoints + " - " + e.getMessage());
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

    public void setTempFileLocation(String tempFileLocation) {
        this.tempFileLocation = tempFileLocation;
    }

    public void setOutputFileLocation(String outputFileLocation) {
        this.outputFileLocation = outputFileLocation;
    }
}
