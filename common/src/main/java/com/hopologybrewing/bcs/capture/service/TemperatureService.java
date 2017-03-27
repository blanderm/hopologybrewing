package com.hopologybrewing.bcs.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbe;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class TemperatureService extends BcsService {
    private static final Logger log = LoggerFactory.getLogger(TemperatureService.class);
    private String fileLocation;
    private DbService dbService;

    public TemperatureService() {}

    public TemperatureService(String user, String pwd) {
        super(user, pwd);
    }

    public TemperatureService(String user, String pwd, String ip) {
        super(user, pwd, ip);
    }

    public Map<String, List<List>> getHistoricalProbeDataFromFile() {
        String line;
        BufferedReader reader = null;
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<List>> probesMap = new HashMap<>();

        try {
            TemperatureProbeRecording probeRecording = null;
            reader = new BufferedReader(new FileReader(fileLocation));

            try {
                while ((line = reader.readLine()) != null) {
                    probeRecording = mapper.readValue(line, TemperatureProbeRecording.class);

                    if (probeRecording != null && probeRecording.getData() != null) {
                        addDataPoint(probesMap, probeRecording.getData().getName(), probeRecording.getTimestamp(), probeRecording.getData().getTemp() / 10);

                        // skip points where the SP isn't set
                        if (probeRecording.getData().getSetpoint() > 0) {
                            addDataPoint(probesMap, probeRecording.getData().getName() + "-SP", probeRecording.getTimestamp(), probeRecording.getData().getSetpoint() / 10);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file " + fileLocation + " - ", e);
            }
        } catch (FileNotFoundException e) {
            log.error("File not found for " + fileLocation + " - ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close reader for " + fileLocation + " - ", e);
                }
            }
        }

        return probesMap;
    }

    public Map<String, List<List>> getHistoricalProbeData(long lower, long upper, int limit) {
        Map<String, List<List>> probesMap = new HashMap<>();
        List<Recording> recordings = null;
        TemperatureProbeRecording probeRecording = null;
        List<TemperatureProbe> probes = getEnabledProbes();
        for (TemperatureProbe probe : probes) {
            // todo: too much leakage from dynamo here, should be able to provide the bean and the service figures it out
            // todo: should make a persistence service and have dynamo and file versions to facilitate both flows and abstract AWS
            recordings = dbService.queryRecording(TemperatureProbeRecording.class, probe.getName(), lower, upper, limit, true);

            for (Recording recording : recordings) {
                if (recording instanceof TemperatureProbeRecording) {
                    probeRecording = (TemperatureProbeRecording) recording;

                    addDataPoint(probesMap, probeRecording.getData().getName(), recording.getTimestamp(), probeRecording.getData().getTemp() / 10);

                    // skip points where the SP isn't set
                    if (probeRecording.getData().getSetpoint() > 0) {
                        addDataPoint(probesMap, probeRecording.getData().getName() + "-SP", recording.getTimestamp(), probeRecording.getData().getSetpoint() / 10);
                    }
                }
            }
        }

        return probesMap;
    }

    private void addDataPoint(Map<String, List<List>> probesMap, String name, Date timestamp, double value) {
        List<List> recordings = probesMap.get(name);

        if (recordings == null) {
            recordings = new ArrayList<>();
        }

        List data = new ArrayList<>();
        data.add(timestamp);
        data.add(value);

        recordings.add(data);
        probesMap.put(name, recordings);
    }

    public List<TemperatureProbe> getEnabledProbes() {
        TemperatureProbe probe = null;
        ResponseEntity<TemperatureProbe> response = null;
        List<TemperatureProbe> probes = new ArrayList<TemperatureProbe>();

        try {
            for (int i = 0; i < BcsConstants.TEMP_PROBE_COUNT; i++) {
                probe = getProbe(String.valueOf(i));

                if (probe != null && probe.isEnabled()) {
                    probes.add(probe);
                }
            }
        } catch (Throwable t) {
            log.error("Error getting temps - ", t);
        }

        return probes;
    }

    public TemperatureProbe getProbe(String probeId) {
        return (TemperatureProbe) super.getData(BcsService.Type.TEMP, probeId);
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}
