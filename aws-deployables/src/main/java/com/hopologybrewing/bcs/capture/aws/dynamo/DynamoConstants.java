package com.hopologybrewing.bcs.capture.aws.dynamo;

import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ddcbryanl on 1/18/17.
 */
public class DynamoConstants {
    public static final String TEMPERATURE_READINGS_TABLE = "temperature_readings";
    public static final String OUTPUT_READINGS_TABLE = "output_readings";
    public static final Map<String, Class> tableRecordingMap = new HashMap<>();
    public static final Map<Class, String> recordingTableMap = new HashMap<>();

    static {
        tableRecordingMap.put(TEMPERATURE_READINGS_TABLE, TemperatureProbeRecording.class);
        tableRecordingMap.put(OUTPUT_READINGS_TABLE, OutputRecording.class);

        recordingTableMap.put(TemperatureProbeRecording.class, TEMPERATURE_READINGS_TABLE);
        recordingTableMap.put(OutputRecording.class, OUTPUT_READINGS_TABLE);
    }
}
