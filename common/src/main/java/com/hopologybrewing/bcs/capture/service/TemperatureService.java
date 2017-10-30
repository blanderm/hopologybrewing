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
import java.util.concurrent.ExecutionException;

@Service
public class TemperatureService extends BcsService {
    private static final Logger log = LoggerFactory.getLogger(TemperatureService.class);
    private DbService dbService;

    public TemperatureService() {
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
        Map<String, List<List>> probesMap = new HashMap<>();
        TemperatureProbeRecording probeRecording = null;
        List<Recording> recordings = dbService.findTemperatureReadings(brewDate, lowerRange, upperRange);

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
        List<TemperatureProbe> enabledProbes = new ArrayList<TemperatureProbe>();
        List probes = (List) super.getData(BcsService.Type.TEMPS);

        if (probes != null) {
            for (int i = 0; i < probes.size(); i++) {
                if (probes.get(i) != null) {
                    enabledProbes.add(getProbe(String.valueOf(i)));
                }
            }
        }

        return enabledProbes;
    }

    public TemperatureProbe getProbe(String probeId) {
        return (TemperatureProbe) super.getData(BcsService.Type.TEMP, probeId);
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}
